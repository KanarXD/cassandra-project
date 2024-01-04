package edu.put.apps;

import com.datastax.driver.mapping.MappingManager;
import edu.put.backend.BackendSession;
import edu.put.dto.OrderInProgress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Random;

@Slf4j
@RequiredArgsConstructor
public class DeliveryApplication extends Thread {
    private final int deliveryAppId;
    private final BackendSession session;
    private final Random random = new Random();
    private MappingManager mappingManager;

    @Override
    public void run() {
        try {
            mappingManager = new MappingManager(session.getSession());

            for (int i = 0; true; i++) {
                Thread.sleep(random.nextInt(50));
                var orderInProgress = getOrderInProgress();
                deleteOrderInProgress(orderInProgress);

                String id = deliveryAppId + ":" + i;
                log.info("add ready order: {}", id);
                addReadyOrder(orderInProgress, id);
            }
        }
        catch (InterruptedException e) {
            log.warn("Thread: {} Interrupted, waring: {}", deliveryAppId, e.getMessage());
        }catch (Exception e) {
            log.error("Thread: {}, error: {}", deliveryAppId, e.getMessage());
        }

    }

    private void addReadyOrder(OrderInProgress orderInProgress, String id) {
        var query = String.format(
                "INSERT INTO ready_orders (id, info) VALUES ('%s', '%s')",
                id,
                orderInProgress
        );
        session.execute(query);
    }

    private void deleteOrderInProgress(OrderInProgress orderInProgress) {
        var query = String.format(
                "DELETE FROM orders_in_progress WHERE orderId='%s' AND creationTime='%s'",
                orderInProgress.getOrderId(),
                orderInProgress.getCreationTime().toInstant()
        );
        session.execute(query);
    }

    private OrderInProgress getOrderInProgress() throws InterruptedException {
        while (true) {
            var orderInProgress = getTopOrders();
            OrderInProgress order;
            if(!orderInProgress.isEmpty()) {
                order = orderInProgress.get(random.nextInt(orderInProgress.size()));
            var query = String.format("SELECT * FROM orders_in_progress WHERE orderId = '%s';", order.getOrderId());
            if (!session.execute(query).all().isEmpty()) {
                return order;
            }
            }
        }
    }

    private List<OrderInProgress> getTopOrders() throws InterruptedException {
        List<OrderInProgress> orderInProgress;
        do {
            var query = "SELECT * FROM orders_in_progress LIMIT 10;";
            var resultSet = session.execute(query);
            var mapper = mappingManager.mapper(OrderInProgress.class);
            orderInProgress = mapper.map(resultSet).all();
            Thread.sleep(50);
        } while (orderInProgress == null);
        return orderInProgress;
    }

}
