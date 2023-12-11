package edu.put.apps;

import edu.put.backend.CassandraSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class ClientApplication extends Thread {
    private final int clientId;
    private final CassandraSession session;

    @Override
    public void run() {
        try {
//            var config = new BackendConfig();
//            var backendSession = new BackendSession(config.getContactPoint(), config.getKeyspace());
//            session = backendSession.getSession();
            for (int i = 0; i < 100; i++) {
                insertClientOrder();
//                sleep(1000);

//            Thread.sleep(100);
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
