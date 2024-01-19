package edu.put.apps;

import com.datastax.oss.driver.api.core.NoNodeAvailableException;
import edu.put.database.dao.DAO;
import edu.put.database.entities.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
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
    private final int id;
    private final DAO mapper;
    private final List<String> categories;
    private final Random random = new Random();
    private Instant last_requested_timestamp = Instant.ofEpochSecond(0);
    private Instant last_timestamp = Instant.ofEpochSecond(0);
    private final List<String> not_delivered_orders = new ArrayList<>();
    private final List<UnconfirmedOrder> orders = new ArrayList<>();
    private List<String> last_orders = List.of();
    private long offset = 5000;

    @SneakyThrows
    @Override
    public void run() {
//        for (var category : categories) {
//          mapper.restaurants().insert(new Restaurant(category, id));
//        }
        mapper.restaurants().insert(new Restaurant(categories.get(id % categories.size()), id));

        while (true) {
            var requested = get_last_orders();
            if (!requested.isEmpty()) {
                last_requested_timestamp = requested.get(0).timestamp();
            }
            requested.removeIf(order -> {
                if (order.timestamp().isAfter(last_timestamp)) {
                    last_timestamp = order.timestamp();
                }
                return last_orders.contains(order.order_id());
            });
            if (last_requested_timestamp.isAfter(last_timestamp.minus(5, ChronoUnit.SECONDS))) {
                last_timestamp = last_requested_timestamp;
            } else {
                last_timestamp = last_timestamp.minus(5, ChronoUnit.SECONDS);
            }
            last_orders = requested.stream().map(Ordered::order_id).toList();

            if (requested.isEmpty() && Thread.currentThread().isInterrupted()) {
                // No new orders were placed, and we received finish signal, so we can finish processing.
                log.info("Restaurant #{} ({}) received interrupt signal. Shutting down.", id, String.join(", ", categories));
                break;
            }

            var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (var order : requested) {
                var date = formatter.format(LocalDate.now());
                var ready = new Ready(date, Instant.now(), order.order_id(), order.order());
                insert_ready(new UnconfirmedOrder(Instant.now(), ready));
                mapper.confirm_order().insert(new OrderConfirmation(order.order_id(), id));
            }

//            confirm_deliveries();
            Thread.sleep(random.nextInt(100));
        }

        // Confirm deliveries while they're pending.
        while (!orders.isEmpty()) {
            try {
                confirm_deliveries();
//                retry_orders();
                Thread.sleep(Math.min(offset, 500));
            } catch (InterruptedException e) {
                // If thread was interrupted twice it will finish without waiting for all confirmations.
                break;
            }
        }
    }

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

    private void confirm_deliveries() {
        orders.removeIf(this::confirm_delivery);
    }

    private boolean confirm_delivery(UnconfirmedOrder order) {
        try {
            var couriers = mapper.confirm_delivery().get(order.order().order_id()).all().stream().toList();
            if (couriers.isEmpty()) {
                return false;
            }

            var courier = couriers.get(random.nextInt(couriers.size()));
            var delivery = new Delivered(courier.order_id(), courier.delivery_id(), courier.order());
            if (mapper.delivery().insert(delivery)) {
                return true;
            }
        } catch (NoNodeAvailableException error) {
            log.warn("Couldn't confirm delivery. Cause: {}", String.join("\n", Arrays.stream(error.getStackTrace()).map(Object::toString).toList()));
        }

        return false;
    }

    private void insert_ready(UnconfirmedOrder order) {
        try {
            orders.add(order);
            mapper.ready().insert(order.order());
        } catch (NoNodeAvailableException error) {
            log.warn("Couldn't insert ready order. Cause: {}", String.join("\n", Arrays.stream(error.getStackTrace()).map(Object::toString).toList()));
        }
    }

    private void retry_orders() {
        var time = Instant.now();
        var reinsert = new ArrayList<UnconfirmedOrder>();
        var remove = new ArrayList<UnconfirmedOrder>();

        for (var order : orders) {
            if (order.timestamp().plus(Duration.ofMillis(offset)).isBefore(time)) {
                reinsert.add(new UnconfirmedOrder(time, order.order()));
                remove.add(order);
            }
        }

        for (var order : reinsert) {
            insert_ready(order);
        }

        orders.removeIf(remove::contains);
        orders.addAll(reinsert);
    }

    private List<Ordered> get_last_orders() {
        return mapper.orders().get(id, last_timestamp).all();
    }

    // @formatter:off
    record UnconfirmedOrder(Instant timestamp, Ready order) {}
    // @formatter:on
}