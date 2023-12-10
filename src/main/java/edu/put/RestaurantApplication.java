package edu.put;

import com.datastax.driver.mapping.MappingManager;
import edu.put.backend.BackendConfig;
import edu.put.backend.BackendSession;
import edu.put.dto.ClientOrder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RestaurantApplication {
    private static int restaurantId;
    private static MappingManager mappingManager;
    private static BackendSession backendSession;

    public static void main(String[] args) throws Exception {
        restaurantId = 0;
        var config = new BackendConfig();
        backendSession = new BackendSession(config.getContactPoint(), config.getKeyspace());
        mappingManager = new MappingManager(backendSession.getSession());

        var foodCategory = "kebab";

        for (int i = 0; i < 100; i++) {
            var clientOrder = getClientOrders(foodCategory);
            log.info("Client order: {}", clientOrder);
            deleteClientOrder(clientOrder);
//            Thread.sleep(100);
            insertOrderInProgress(clientOrder);
        }

        System.exit(0);
    }

    private static void deleteClientOrder(ClientOrder clientOrder) {
        var query = String.format(
                "DELETE FROM client_orders WHERE foodCategory='%s' AND creationTime='%s' AND orderId='%s'",
                clientOrder.getFoodCategory(),
                clientOrder.getCreationTime().toInstant(),
                clientOrder.getOrderId()
        );
        backendSession.execute(query);
    }

    private static void insertOrderInProgress(ClientOrder clientOrder) {
        var query = String.format(
                "INSERT INTO orders_in_progress (orderId, creationTime, status, restaurantId, info) VALUES ('%s', '%s', '%s', %s, '%s')",
                clientOrder.getOrderId(),
                clientOrder.getCreationTime().toInstant(),
                "READY",
                restaurantId,
                clientOrder
        );
        backendSession.execute(query);
    }

    private static ClientOrder getClientOrders(String foodCategory) {
        var query = String.format("SELECT * FROM client_orders WHERE foodCategory='%s' LIMIT 1;", foodCategory);
        var resultSet = backendSession.execute(query);
        var mapper = mappingManager.mapper(ClientOrder.class);
        return mapper.map(resultSet).one();
    }
}
