package edu.put;

import edu.put.backend.BackendConfig;
import edu.put.backend.BackendSession;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.UUID;

@Slf4j
public class ClientApplication {

    public static void main(String[] args) throws Exception {
        var config = new BackendConfig();
        var backendSession = new BackendSession(config.getContactPoint(), config.getKeyspace());
        var session = backendSession.getSession();
        for (int i = 0; i < 100; i++) {
            String query = String.format(
                    "INSERT INTO client_orders (food_category, creation_time, user_id, food, order_id) VALUES ('%s', '%s', %s, '%s', '%s')",
                    "kebab",
                    new Date().toInstant(),
                    0,
                    "plus size",
                    UUID.randomUUID()
            );
            log.info("Running query: {}", query);
            session.execute(query);
        }

        System.exit(0);
    }

}
