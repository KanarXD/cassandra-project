package edu.put.apps;

import edu.put.backend.BackendSession;
import edu.put.backend.Common;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Random;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class ClientApplication extends Thread {
    private final int clientId;
    private final BackendSession session;
    private final Random random = new Random();

    @Override
    public void run() {
        try {
            for (int i = 0; i < 100; i++) {
                insertClientOrder();
                Thread.sleep(100);
            }
        } catch (Exception e) {
            log.error("Thread: {}, error: {}", clientId, e.getMessage());
        }
    }

    private void insertClientOrder() {
        var foodMap = Common.foodMap;
        var foodCategory = foodMap.keySet().stream().toList().get(random.nextInt(foodMap.keySet().size()));
        var food = foodMap.get(foodCategory).get(random.nextInt(foodMap.get(foodCategory).size()));
        var query = String.format(
                "INSERT INTO client_orders (foodCategory, creationTime, userId, food, orderId) VALUES ('%s', '%s', %s, '%s', '%s')",
                foodCategory,
                new Date().toInstant(),
                clientId,
                food,
                UUID.randomUUID()
        );
        log.info("Running query: {}", query);
        session.execute(query);
    }

}
