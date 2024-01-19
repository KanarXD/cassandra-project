package edu.put.apps;

import com.datastax.oss.driver.api.core.NoNodeAvailableException;
import edu.put.backend.Foods;
import edu.put.database.dao.DAO;
import edu.put.database.entities.Order;
import edu.put.database.entities.Ordered;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
public class ClientApplication extends Thread {
    // Statistics.
    private static int missed_writes = 0;
    private static int missed_reads = 0;

    // App Configuration parameters.
    private final int id;
    private final DAO mapper;

    private long offset = 5000;

    // Internal app state.
    private final Random random = new Random();
    private final List<UnconfirmedOrder> orders = new ArrayList<>();

    /**
     * @return number of failed writes performed by all clients.
     */
    public int missed_writes() {
        return missed_writes;
    }

    /**
     * @return number of failed reads performed by all clients.
     */
    public int missed_reads() {
        return missed_reads;
    }

    public ClientApplication(int id, DAO mapper) {
        this.id = id;
        this.mapper = mapper;
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


    @Override
    public void run() {
        log.info("start client: {}", id);
        try {
            for (int i = 0; i < 100; i++) {
                var order = prepare_order();
                make_order(order);
//                retry_pending_orders();
                Thread.sleep(random.nextInt(100));
            }

//            while (retry_pending_orders()) {
//                Thread.sleep(offset);
//            }
            log.trace("Client #{} made all their orders.", id);

        } catch (Exception error) {
            log.error("Client #{} failed: {}", id, error.getMessage());
        }
    }

    private Order prepare_order() {
        try {
            var category = Foods.categories().get(random.nextInt(Foods.categories().size()));
            var food = Foods.variants(category).get(random.nextInt(Foods.variants(category).size()));
            var order_id = UUID.randomUUID().toString();
            return new Order(order_id, category, food, id);
        } catch (NullPointerException | IllegalArgumentException | IndexOutOfBoundsException error) {
            log.error("Empty categories or category variants.");
            System.exit(1);
        }

        // NOTE: Unreachable in practice.
        return null;
    }

    private void check_confirmed() {
        try {
            var remove = new ArrayList<UnconfirmedOrder>();
            for (var order : orders) {
                if (mapper.confirm_order().get(order.order().id()) != null) {
                    remove.add(order);
                }
            }

            orders.removeIf(remove::contains);

        } catch (NoNodeAvailableException error) {
            log.warn("Couldn't confirm delivery. Cause: {}", String.join("\n", Arrays.stream(error.getStackTrace()).map(Object::toString).toList()));
        }
    }

    private void make_order(Order order) {
        orders.add(new UnconfirmedOrder(Instant.now(), order));

        Optional<Integer> restaurant;
        do {
            restaurant = Optional.ofNullable(get_restaurant(order.category()));
        } while (restaurant.isEmpty());

//        if (restaurant != null) {
        var ordered = new Ordered(restaurant.get(), Instant.now(), order.id(), order);
        try {
            log.trace("Client ${} inserting ordered: {}", id, ordered);
            if (!mapper.orders().insert(ordered)) {
                log.warn("Order `{}` couldn't be inserted.", order);
                synchronized (this) {
                    missed_writes += 1;
                }
            }
        } catch (NoNodeAvailableException error) {
            log.warn("Client #{} failed to make order. Cause: {}", id, error.getMessage());
            synchronized (this) {
                missed_writes += 1;
            }
        } catch (Exception error) {
            log.warn("Client #{} failed to make order. Cause: {}", id, error.getMessage());
            throw error;
        }
//        } else {
//            log.warn("Client #{} failed to make order no restaurants", id);
//        }
    }

//    private void insert_order(Order order) {
//        try {
//            var restaurants = mapper.restaurants().get(order.category()).all().stream().toList();
//            if (restaurants.isEmpty()) {
//                log.warn("No restaurants available for performing order: `{}`.", order);
//                synchronized (this) {
//                    uninserted += 1;
//                }
//                return;
//            }
//            var restaurant = restaurants.get(random.nextInt(restaurants.size()));
//
//            insert_order(order, restaurant.restaurant_id());
//        } catch (NoNodeAvailableException error) {
//            log.warn("Client #{} encountered problem (probably overloaded cassandra nodes): {}", id, error.getMessage());
//        } catch (Exception error) {
//            log.warn("Client #{} encountered error: {}", id, error.getStackTrace());
//        }
//    }

    /**
     * This is implementation of timeout-retry policy of orders.
     * Every order made (or failed to made) is held in local list with their insertion time.
     * If insertion time is older than retry offset we're reinserting order into database.
     *
     * @return whether there are orders to retry yet.
     */
    boolean retry_pending_orders() {
        var time = Instant.now();
        var reinsert = new ArrayList<UnconfirmedOrder>();
        var remove = new ArrayList<UnconfirmedOrder>();

        for (var order : orders) {
            if (order.timestamp().plus(Duration.ofMillis(offset)).isBefore(time)) {
                reinsert.add(new UnconfirmedOrder(time, order.order()));
                remove.add(order);
            }
        }

        orders.removeIf(remove::contains);

        for (var order : reinsert) {
            make_order(order.order());
        }

        return !orders.isEmpty();
    }

    /**
     * Chooses from database restaurant that prepares meals from given category.
     *
     * @param category meal category prepared in restaurant
     * @return ID of restaurant or `null` if no restaurant with given category was found.
     */
    Integer get_restaurant(String category) {
        try {
            var restaurants = mapper.restaurants().get(category).all();
            if (restaurants.isEmpty()) {
                synchronized (this) {
                    missed_reads += 1;
                }
                return null;
            }
            var index = random.nextInt(restaurants.size());

            return restaurants.get(index).restaurant_id();
        } catch (NoNodeAvailableException error) {
            log.warn("Client #{} failed to retrieve restaurants. Cause: {}", id, error.getMessage());
            synchronized (this) {
                missed_reads += 1;
            }
        } catch (Exception error) {
            log.warn("Client #{} failed to retrieve restaurants. Cause: {}", id, error.getMessage());
            throw error;
        }

        return null;
    }

    // @formatter:off
    record UnconfirmedOrder(Instant timestamp, Order order) {}
    // @formatter:on
}
