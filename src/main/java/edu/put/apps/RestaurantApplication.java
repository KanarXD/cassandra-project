package edu.put.apps;

//import edu.put.backend.Foods;

import edu.put.database.dao.DAO;
import edu.put.database.entities.Delivered;
import edu.put.database.entities.Ordered;
import edu.put.database.entities.Ready;
import edu.put.database.entities.Restaurant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@RequiredArgsConstructor
public class RestaurantApplication extends Thread {
    private final int id;
    private final DAO mapper;
    private final List<String> categories;
    private final Random random = new Random();
    private final Instant last_timestamp = Instant.ofEpochSecond(0);
    private final List<String> not_delivered_orders = new ArrayList<>();
    private List<String> last_orders = List.of();

    @Override
    public void run() {
        for (var category : categories) {
            mapper.restaurants().insert(new Restaurant(category, id));
        }

        while (true) {
            var orders = get_last_orders();
            orders.removeIf(order -> last_orders.contains(order.order_id()));
            last_orders = orders.stream().map(Ordered::order_id).toList();

            if (orders.isEmpty() && Thread.interrupted()) {
                // No new orders were placed, and we received finish signal, so we can finish processing.
                log.info("Restaurant #{} ({}) received interrupt signal. Shutting down.", id, String.join(", ", categories));
                break;
            }

            var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (var order : orders) {
                var date = formatter.format(LocalDate.now());
                var ready = new Ready(date, Instant.now(), order.order_id(), order.order());
                if (mapper.ready().insert(ready)) {
                    // We're doing check on insertion to avoid infinite confirmation loop.
                    not_delivered_orders.add(order.order_id());
                }
            }

            //noinspection ResultOfMethodCallIgnored
            confirm_deliveries();
        }

        // Confirm deliveries while they're pending.
        while (confirm_deliveries()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // If thread was interrupted twice it will finish without waiting for all confirmations.
                break;
            }
        }
    }

    private boolean confirm_deliveries() {
        var finished = new ArrayList<String>();
        for (var order : not_delivered_orders) {
            var couriers = mapper.confirm().get(order).all().stream().toList();
            if(couriers.isEmpty()) {
                continue;
            }
            var courier = couriers.get(random.nextInt(couriers.size()));
            var delivery = new Delivered(courier.delivery_id(), courier.order_id(), courier.order());
            if (mapper.delivery().insert(delivery)) {
                finished.add(order);
            }
        }

        not_delivered_orders.removeIf(finished::contains);
        return !not_delivered_orders.isEmpty();
    }

    private List<Ordered> get_last_orders() {
        return mapper.orders().get(id, last_timestamp).all();
    }
}
