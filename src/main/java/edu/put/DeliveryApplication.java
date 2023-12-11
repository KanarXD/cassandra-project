package edu.put;

import com.datastax.driver.mapping.MappingManager;
import edu.put.backend.BackendConfig;
import edu.put.backend.BackendSession;
import edu.put.dto.OrderInProgress;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeliveryApplication {
    private static int deliveryAppId;
    private static MappingManager mappingManager;
    private static BackendSession backendSession;

    public static void main(String[] args) throws Exception {
        deliveryAppId = 0;
        var config = new BackendConfig();
        backendSession = new BackendSession(config.getContactPoint(), config.getKeyspace());
        mappingManager = new MappingManager(backendSession.getSession());

        for (int i = 0; i < 100; i++) {
            var orderInProgress = getOrderInProgress();
            deleteOrderInProgress(orderInProgress);

            String id = deliveryAppId + ":" + i;
            addReadyOrder(orderInProgress, id);
        }

        System.exit(0);
    }

    private static void addReadyOrder(OrderInProgress orderInProgress, String id) {
        var query = String.format(
                "INSERT INTO ready_orders (id, info) VALUES ('%s', '%s')",
                id,
                orderInProgress
        );
        backendSession.execute(query);
    }

    private static void deleteOrderInProgress(OrderInProgress orderInProgress) {
        var query = String.format(
                "DELETE FROM orders_in_progress WHERE orderId='%s' AND creationTime='%s'",
                orderInProgress.getOrderId(),
                orderInProgress.getCreationTime().toInstant()
        );
        backendSession.execute(query);
    }

    private static OrderInProgress getOrderInProgress() {
        var query = "SELECT * FROM orders_in_progress LIMIT 1;";
        var resultSet = backendSession.execute(query);
        var mapper = mappingManager.mapper(OrderInProgress.class);
        return mapper.map(resultSet).one();
    }

}
