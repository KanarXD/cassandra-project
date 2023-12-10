package edu.put;

import com.datastax.driver.core.Session;
import edu.put.backend.BackendConfig;
import edu.put.backend.BackendSession;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.UUID;

@Slf4j
public class ClientApplication {
    private static int clientId;
    private static Session session;

    public static void main(String[] args) throws Exception {
        clientId = 0;
        var config = new BackendConfig();
        var backendSession = new BackendSession(config.getContactPoint(), config.getKeyspace());
        session = backendSession.getSession();
        for (int i = 0; i < 100; i++) {
            insertClientOrder();
//            Thread.sleep(100);
        }

        System.exit(0);
    }

    private static void insertClientOrder() {
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
