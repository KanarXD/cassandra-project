package edu.put.apps;

import edu.put.database.dao.DAO;
import edu.put.database.entities.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Slf4j
@RequiredArgsConstructor
public class RestaurantApplication extends Thread {
    private static final Object mutex = new Object();
    // Statistics
    public static int total_writes = 0;
    public static int missed_writes = 0;
    public static int missed_reads = 0;

    // App configuration parameters.
    private final int id;
    private final DAO mapper;
    private final List<String> categories;

    private long offset = 5000;

    // Internal app state.
    private final Random random = new Random();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final List<UnconfirmedOrder> orders = new ArrayList<>();
    private List<String> last_orders = List.of();
    private Instant last_timestamp = Instant.ofEpochSecond(0);
    private boolean finish_requested = false;

    /**
     * Sets time offset after which retry should be performed.
     *
     * @param offset time in milliseconds
     * @return modified instance of RestaurantApplication
     */
    public RestaurantApplication with_retry_offset(long offset) {
        this.offset = offset;
        return this;
    }

    @Override
    public void run() {
        try {
//            for (var category : categories) {
//                mapper.restaurants().insert(new Restaurant(category, id));
//            }
            mapper.restaurants().insert(new Restaurant(categories.get(id % categories.size()), id));

            while (true) {
                for (var order : get_last_orders()) {
                    confirm_order(order);
                    make_ready(order);
                }

                confirm_deliveries();
                retry_pending_orders();

                if (orders.isEmpty() && finish_requested) {
                    break;
                }

                try {
                    Thread.sleep(random.nextInt(1000));
                } catch (InterruptedException e) {
                    finish_requested = true;
                }
            }
        } catch (Exception e) {
            log.error("Restaurant #{} failed. Error: {}", id, e.getMessage());
        }
    }

    private void make_ready(Ordered order) {
        var date = formatter.format(LocalDate.now());
        var ready = new Ready(date, Instant.now(), order.order_id(), order.order());
        insert_ready(ready);
    }

    private void confirm_order(Ordered order) {
        try {
            increase_total_writes();
            mapper.confirm_order().insert(new OrderConfirmation(order.order_id(), id));
        } catch (Exception error) {
            log.warn("Couldn't insert order confirmation. Cause: {}", String.join("\n", Arrays.stream(error.getStackTrace()).map(Object::toString).toList()));
            synchronized (mutex) {
                missed_writes += 1;
            }
        }
    }

    private void confirm_deliveries() {
        orders.removeIf(this::confirm_delivery);
    }

    private boolean confirm_delivery(UnconfirmedOrder order) {
        try {
            var delivery = prepare_delivery(order);
            if (delivery == null) return false;
            increase_total_writes();
            if (mapper.delivery().insert(delivery)) {
                return true;
            }
        } catch (Exception error) {
            log.warn("Couldn't confirm delivery. Cause: {}", String.join("\n", Arrays.stream(error.getStackTrace()).map(Object::toString).toList()));
        }

        return false;
    }

    private Delivered prepare_delivery(UnconfirmedOrder order) {
        try {
            var couriers = mapper.confirm_delivery().get(order.order().order_id()).all();
            if (couriers.isEmpty()) {
                return null;
            }
            var courier = couriers.get(random.nextInt(couriers.size()));

            return new Delivered(courier.order_id(), courier.delivery_id(), courier.order());
        } catch (Exception error) {
            log.warn("Couldn't confirm delivery. Cause: {}", String.join("\n", Arrays.stream(error.getStackTrace()).map(Object::toString).toList()));
            synchronized (mutex) {
                missed_reads += 1;
            }
        }
        return null;
    }

    private void insert_ready(Ready order) {
        try {
            orders.add(new UnconfirmedOrder(Instant.now(), order));
            var ttl = (int) Math.max(1.0f, (((float) offset / 1000.0f) + 0.5f));
//            var ttl = 3600;
            increase_total_writes();
            mapper.ready().insert(order, ttl);
        } catch (Exception error) {
            log.warn("Couldn't insert ready order. Cause: {}", String.join("\n", Arrays.stream(error.getStackTrace()).map(Object::toString).toList()));
            synchronized (mutex) {
                missed_writes += 1;
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
            insert_ready(order.order());
        }
    }

    /**
     * Retrieves orders from database, removing duplicates.
     *
     * @return list of new orders pushed since the last call.
     */
    private List<Ordered> get_last_orders() {
        try {
            var requested = mapper.orders().get(id, last_timestamp).all();
            requested.removeIf(order -> last_orders.contains(order.order_id()));

            if (!requested.isEmpty()) {
                // orders should be sorted by timestamp, so we need to get first and last elements.
                Instant first = requested.get(0).timestamp();
                Instant last = requested.get(requested.size() - 1).timestamp().minus(5, ChronoUnit.SECONDS);

                last_timestamp = first.isAfter(last) ? first : last;
                last_orders = requested.stream().map(Ordered::order_id).toList();
            }

            return requested;
        } catch (Exception error) {
            log.warn("Couldn't retrieve last orders. Cause: {}", String.join("\n", Arrays.stream(error.getStackTrace()).map(Object::toString).toList()));
            synchronized (mutex) {
                missed_reads += 1;
            }
        }

        return List.of();
    }

    private void increase_total_writes() {
        synchronized (mutex) {
            total_writes++;
        }
    }
    // @formatter:off

    record UnconfirmedOrder(Instant timestamp, Ready order) {}
    // @formatter:on
}