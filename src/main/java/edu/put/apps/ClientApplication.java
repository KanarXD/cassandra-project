package edu.put.apps;

import edu.put.backend.BackendSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class ClientApplication extends Thread {
    private final int clientId;
    private final BackendSession session;
    private final Random random = new Random();
    private final Map<String, List<String>> foodMap = Map.of(
            "kebab", List.of("doner", "small", "medium", "large", "xxl", "spicy"),
            "pizza", List.of("margherita", "neapolitana", "jalapeno", "piccante", "salami", "hawaii"),
            "drink", List.of("tequila sunrise", "sex on the beach", "blue lagoon", "margarita", "cosmopolitan")
    );

    @Override
    public void run() {
        try {
            for (int i = 0; i < 100; i++) {
                insertClientOrder();
            }
        } catch (Exception e) {
            log.error("Thread: {}, error: {}", clientId, e.getMessage());
        }
    }

    private void insertClientOrder() {
        var foodCategory = foodMap.keySet().stream().toList().get(random.nextInt(foodMap.size()));
        var food = foodMap.get(foodCategory);
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
