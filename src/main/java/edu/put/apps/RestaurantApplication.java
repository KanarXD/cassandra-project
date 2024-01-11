package edu.put.apps;

import com.datastax.driver.mapping.MappingManager;
import edu.put.backend.BackendSession;
import edu.put.backend.Common;
import edu.put.dto.ClientOrder;
import edu.put.dto.OrderInProgress;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Slf4j
@RequiredArgsConstructor
public class RestaurantApplication extends Thread {
    private final int id;
    private final BackendSession session;
    private Date lastInProgress = new Date();
    private final Random random = new Random();
    private MappingManager mapping;

    @SneakyThrows
    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {
        var category = Common.foodMap.keySet().stream().toList().get(id % Common.foodMap.size());

        try {
            mapping = new MappingManager(session.session());

            for (int i = 0; true; i++) {
                var order = get_order(category);
                delete_client_order(order);
                create_order_confirmation(order);
                add_ready_orders();

                log.debug("Restaurant #{} | Client order #{} is: {}", id, i, order);
            }

        } catch (InterruptedException e) {
            Thread.interrupted();
            var count = getOrdersInProgressCount();
            while (add_ready_orders()) {
                log.debug("No more client orders, waiting for confirmations");
                Thread.sleep(5000);
                if (count == getOrdersInProgressCount()) {
                    break;
                }
            }
            log.info("Restaurant #{} (category: {}) received interrupt signal. Shutting down.", id, category);
        } catch (Exception e) {
            log.error("Restaurant #{} failed. Error: {}", id, e.getMessage());
            e.printStackTrace();
        }
    }

    private long getOrdersInProgressCount() {
        var query = String.format("SELECT COUNT(*) FROM orders_in_progress WHERE restaurant_id=%d ", id);
        var results = session.execute(query);
        return results.one().getLong("count");
    }

    private boolean add_ready_orders() {
        var mapper = mapping.mapper(OrderInProgress.class);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(lastInProgress);
        calendar.add(Calendar.SECOND, -5);

        var query = String.format("SELECT * FROM orders_in_progress WHERE restaurant_id=%d AND creation_time > '%s'", id, calendar.toInstant());
        var results = session.execute(query);
        if (results == null) {
            log.debug("Nothing in orders_in_progress for restaurant: {}", id);
            return false;
        }
        var inProgresses = mapper.map(results).all();
        if (inProgresses.isEmpty()) {
            return false;
        }
        for (var inProgress : inProgresses) {
            var timestamp = inProgress.getCreationTime();
            if (timestamp.after(lastInProgress)) {
                lastInProgress = timestamp;
            }
            create_ready_order(inProgress.getOrderId(), inProgress.getInfo());
        }
        return true;
    }

    private void create_ready_order(String orderId, String info) {
        var readyOrderId = id + ":" + orderId;
        session.execute(String.format("INSERT INTO ready_orders (id, creation_time, info) VALUES ('%s', '%s', '%s')", readyOrderId, new Date().toInstant(), info));
    }

    private void create_order_confirmation(ClientOrder order) {
        var query = String.format("INSERT INTO order_confirmation (order_id, restaurant_id, info) VALUES ('%s', %d, '%s');", order.getOrderId(), id, order);
        session.execute(query);
    }

    private void delete_client_order(ClientOrder order) {
        session.execute(String.format("DELETE FROM client_orders WHERE food_category='%s' AND creation_time='%s' AND order_id='%s'", order.getCategory(), order.getCreationTime().toInstant(), order.getOrderId()));
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
