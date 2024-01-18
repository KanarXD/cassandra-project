package edu.put.apps;

import com.datastax.oss.driver.api.core.NoNodeAvailableException;
import edu.put.backend.Foods;
import edu.put.database.dao.DAO;
import edu.put.database.entities.Order;
import edu.put.database.entities.Ordered;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class ClientApplication extends Thread {
    private static int uninserted = 0;
    private final int id;
    private final DAO mapper;
    private final Random random = new Random();
    private final List<Pair> ordered = new ArrayList<>();
    private long offset = 5000;

    @Override
    public void run() {
        try {
            for (int i = 0; i < 100; i++) {
                make_order();
                retry_orders();
                Thread.sleep(random.nextInt(100));
            }

            while (retry_orders()) {

                Thread.sleep(offset);
            }
            log.trace("Client #{} made all their orders.", id);

        } catch (Exception error) {
            log.error("Client #{} failed: {}", id, error.getMessage());
        }
    }

    /**
     * Sets time offset after which retry should be performed.
     *
     * @param offset time in milliseconds
     * @return modified instance of ClientApplication
     */
    public ClientApplication with_retry_offset(long offset) {
        this.offset = offset;
        return this;
    }

    /**
     * @return number of failed attempts to made orders.
     */
    public int failure_count() {
        return uninserted;
    }

    private void make_order() {
        var category = Foods.categories().get(random.nextInt(Foods.categories().size()));
        var food = Foods.variants(category).get(random.nextInt(Foods.variants(category).size()));
        var order_id = UUID.randomUUID().toString();
        var order = new Order(order_id, category, food, id);
        ordered.add(new Pair(Instant.now(), order));
        insert_order(order);
    }

    private void check_confirmed() {
        try {
            var remove = new ArrayList<Pair>();
            for (var order : ordered) {
                if (mapper.confirm_order().get(order.order().id()) != null) {
                    remove.add(order);
                }
            }

            ordered.removeIf(remove::contains);

        } catch (NoNodeAvailableException error) {
            log.warn("Couldn't confirm delivery. Cause: {}", String.join("\n", Arrays.stream(error.getStackTrace()).map(Object::toString).toList()));
        }
    }

    private void insert_order(Order order) {
        try {
            var restaurants = mapper.restaurants().get(order.category()).all().stream().toList();
            if (restaurants.isEmpty()) {
                log.warn("No restaurants available for performing order: `{}`.", order);
                synchronized (this) {
                    uninserted += 1;
                }
                return;
            }
            var restaurant = restaurants.get(random.nextInt(restaurants.size()));

            var ordered = new Ordered(restaurant.restaurant_id(), Instant.now(), order.id(), order);

            if (!mapper.orders().insert(ordered)) {
                log.warn("Order `{}` couldn't be inserted.", order);
                synchronized (this) {
                    uninserted += 1;
                }
            }
        } catch (NoNodeAvailableException error) {
            log.warn("Client #{} encountered problem (probably overloaded cassandra nodes): {}", id, error.getMessage());
        } catch (Exception error) {
            log.warn("Client #{} encountered error: {}", id, error.getStackTrace());
        }
    }

    /**
     * This is implementation of timeout-retry policy of orders.
     * Every order made (or failed to made) is held in local list with their insertion time.
     * If insertion time is older than retry offset we're reinserting order into database.
     *
     * @return whether there are orders to retry yet.
     */
    boolean retry_orders() {
        var time = Instant.now();
        var reinsert = new ArrayList<Pair>();
        var remove = new ArrayList<Pair>();

        for (var pair : ordered) {
            if (pair.timestamp().plus(Duration.ofMillis(offset)).isBefore(time)) {
                reinsert.add(new Pair(time, pair.order()));
                remove.add(pair);
            }
        }

        for (var order : reinsert) {
            insert_order(order.order());
        }

        ordered.removeIf(remove::contains);
        ordered.addAll(reinsert);

        return !ordered.isEmpty();
    }
}

// @formatter:off
record Pair(Instant timestamp, Order order) {}
// @formatter:on
