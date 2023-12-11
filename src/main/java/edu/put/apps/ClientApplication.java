package edu.put.apps;

import edu.put.backend.BackendSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class ClientApplication extends Thread {
    private final int clientId;
    private final BackendSession session;

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
        var query = String.format(
                "INSERT INTO client_orders (foodCategory, creationTime, userId, food, orderId) VALUES ('%s', '%s', %s, '%s', '%s')",
                "kebab",
                new Date().toInstant(),
                clientId,
                "plus size",
                UUID.randomUUID()
        );
        log.info("Running query: {}", query);
        session.execute(query);
    }

}
