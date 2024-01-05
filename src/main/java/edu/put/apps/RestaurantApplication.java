package edu.put.apps;

import com.datastax.driver.mapping.MappingManager;
import edu.put.backend.BackendSession;
import edu.put.backend.Common;
import edu.put.dto.ClientOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Random;

@Slf4j
@RequiredArgsConstructor
public class RestaurantApplication extends Thread {
    private final int id;
    private final BackendSession session;
    private final Random random = new Random();
    private MappingManager mapping;

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {
        var category = Common.foodMap.keySet().stream().toList().get(id % Common.foodMap.size());

        try {
            mapping = new MappingManager(session.session());

            for (int i = 0; true; i++) {
                var order = get_order(category);
                delete_client_order(order);
                create_order(order);
                log.debug("Restaurant #{} | Client order #{} is: {}", id, i, order);
            }
        } catch (InterruptedException e) {
            log.info("Restaurant #{} (category: {}) received interrupt signal. Shutting down.", id, category);
        } catch (Exception e) {
            log.error("Restaurant #{} failed. Error: {}", id, e.getMessage());
        }
    }

    private void delete_client_order(ClientOrder order) {
        session.execute(String.format("DELETE FROM client_orders WHERE food_category='%s' AND creation_time='%s' AND order_id='%s'", order.getCategory(), order.getCreationTime().toInstant(), order.getOrderId()));
    }

    private void create_order(ClientOrder order) {
        session.execute(String.format("INSERT INTO orders_in_progress (order_id, creation_time, status, restaurant_id, info) VALUES ('%s', '%s', '%s', %s, '%s')", order.getOrderId(), order.getCreationTime().toInstant(), "READY", id, order));
    }

    private ClientOrder get_order(String category) throws InterruptedException {
        var mapper = mapping.mapper(ClientOrder.class);

        while (true) {
            var orders = get_top_orders(category, 10);
            var order = orders.get(random.nextInt(orders.size()));
            var results = session.execute(String.format("SELECT * FROM client_orders WHERE food_category='%s' AND creation_time='%s' AND order_id='%s';", category, order.getCreationTime().toInstant(), order.getOrderId()));
            if (!mapper.map(results).all().isEmpty()) {
                return order;
            }
        }
    }

    @SuppressWarnings({"SameParameterValue"})
    private List<ClientOrder> get_top_orders(String category, int limit) throws InterruptedException {
        var mapper = mapping.mapper(ClientOrder.class);
        List<ClientOrder> orders = List.of();
        while (orders.isEmpty()) {
            var results = session.execute(String.format("SELECT * FROM client_orders WHERE food_category='%s' LIMIT %d;", category, limit));
            orders = mapper.map(results).all();

            if (orders.isEmpty() && Thread.currentThread().isInterrupted()) {
                // Thread was interrupted, so no new orders are coming and we should finish.
                throw new InterruptedException();
            }
        }

        return orders;
    }
}
