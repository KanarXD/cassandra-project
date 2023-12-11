package edu.put.apps;

import com.datastax.driver.mapping.MappingManager;
import edu.put.backend.CassandraSession;
import edu.put.dto.ClientOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class RestaurantApplication extends Thread {
    private final int restaurantId;
    private final CassandraSession session;
    private MappingManager mappingManager;

    @Override
    public void run() {
        try {

            mappingManager = new MappingManager(session.getSession());

            var foodCategory = "kebab";

            for (int i = 0; i < 100; i++) {
                var clientOrder = getClientOrders(foodCategory);
                log.info("Client order: {}, is: {}", i, clientOrder);
                deleteClientOrder(clientOrder);
//                sleep(1000);

//            Thread.sleep(100);
                insertOrderInProgress(clientOrder);
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

    private ClientOrder getClientOrders(String foodCategory) {
        ClientOrder clientOrder;
        do {
            var query = String.format("SELECT * FROM client_orders WHERE foodCategory='%s' LIMIT 1;", foodCategory);
            var resultSet = session.execute(query);
            var mapper = mappingManager.mapper(ClientOrder.class);
            clientOrder = mapper.map(resultSet).one();
        } while (clientOrder == null);
        return clientOrder;
    }
}
