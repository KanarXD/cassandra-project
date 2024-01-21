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
    private static final Object mutex = new Object();
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
                check_confirmed_orders();
                retry_pending_orders();
                Thread.sleep(random.nextInt(100));
            }

            while (!orders.isEmpty()) {
                check_confirmed_orders();
                retry_pending_orders();
                Thread.sleep(random.nextInt(2000));
            }
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

    private void make_order(Order order) {
        orders.add(new UnconfirmedOrder(Instant.now(), order));

        var restaurant = get_restaurant(order.category());
        if (restaurant != null) {
            var ordered = new Ordered(restaurant, Instant.now(), order.id(), order);
            try {
                log.trace("Client ${} inserting ordered: {}", id, ordered);
                if (!mapper.orders().insert(ordered)) {
                    log.warn("Order `{}` couldn't be inserted.", order);
                    synchronized (mutex) {
                        missed_writes += 1;
                    }
                }
            } catch (NoNodeAvailableException error) {
                log.warn("Client #{} failed to make order. Cause: {}", id, error.getMessage());
                synchronized (mutex) {
                    missed_writes += 1;
                }
            } catch (Exception error) {
                log.warn("Client #{} failed to make order. Cause: {}", id, error.getMessage());
                throw error;
            }
        }
    }

    private void check_confirmed_orders() {
        try {
            orders.removeIf(order -> mapper.confirm_order().get(order.order().id()) != null);
        } catch (Exception error) {
            log.warn("Couldn't confirm delivery. Cause: {}", String.join("\n", Arrays.stream(error.getStackTrace()).map(Object::toString).toList()));
            synchronized (this) {
                missed_reads += 1;
            }
        }
    }

    /**
     * This is implementation of timeout-retry policy of orders.
     * Every order made (or failed to made) is held in local list with their insertion time.
     * If insertion time is older than retry offset we're reinserting order into database.
     */
    void retry_pending_orders() {
        var time = Instant.now();
        var reinsert = new ArrayList<UnconfirmedOrder>();

        var iterator = orders.iterator();
        while (iterator.hasNext()) {
            var order = iterator.next();
            if (order.timestamp().plus(Duration.ofMillis(offset)).isBefore(time)) {
                reinsert.add(new UnconfirmedOrder(time, order.order()));
                iterator.remove();
            }
        }

        for (var order : reinsert) {
            make_order(order.order());
        }
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

