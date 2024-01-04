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
    private final int restaurantId;
    private final BackendSession session;
    private final Random random = new Random();
    private MappingManager mappingManager;

    @Override
    public void run() {
        try {

            mappingManager = new MappingManager(session.getSession());

            var foodCategory = Common.foodMap.keySet().stream().toList().get(restaurantId % Common.foodMap.size());

            for (int i = 0; i < 100; i++) {
                var clientOrder = getClientOrders(foodCategory);
                log.info("Client order: {}, is: {}", i, clientOrder);
                deleteClientOrder(clientOrder);
                insertOrderInProgress(clientOrder);
                Thread.sleep(100);
            }
        } catch (Exception e) {
            log.error("Thread: {}, error: {}", restaurantId, e.getMessage());
        }
    }

    private void deleteClientOrder(ClientOrder clientOrder) {
        var query = String.format(
                "DELETE FROM client_orders WHERE foodCategory='%s' AND creationTime='%s' AND orderId='%s'",
                clientOrder.getFoodCategory(),
                clientOrder.getCreationTime().toInstant(),
                clientOrder.getOrderId()
        );
        session.execute(query);
    }

    private void insertOrderInProgress(ClientOrder clientOrder) {
        var query = String.format(
                "INSERT INTO orders_in_progress (orderId, creationTime, status, restaurantId, info) VALUES ('%s', '%s', '%s', %s, '%s')",
                clientOrder.getOrderId(),
                clientOrder.getCreationTime().toInstant(),
                "READY",
                restaurantId,
                clientOrder
        );
        session.execute(query);
    }

    private ClientOrder getClientOrders(String foodCategory) throws InterruptedException {
        var mapper = mappingManager.mapper(ClientOrder.class);
        List<ClientOrder> clientOrders;
        do {
            var query = String.format("SELECT * FROM client_orders WHERE foodCategory='%s' LIMIT 10;", foodCategory);
            var resultSet = session.execute(query);
            clientOrders = mapper.map(resultSet).all();
            Thread.sleep(5);
        } while (clientOrders == null || clientOrders.isEmpty());
        ClientOrder clientOrder = clientOrders.get(random.nextInt(clientOrders.size()));
        var query = String.format("SELECT * FROM client_orders WHERE foodCategory='%s' AND creationTime='%s' AND orderId='%s';",
                foodCategory, clientOrder.getCreationTime().toInstant(), clientOrder.getOrderId());
        var resultSet = session.execute(query);
        if (mapper.map(resultSet).one() == null) {
            return getClientOrders(foodCategory);
        }
        return clientOrder;
    }
}
