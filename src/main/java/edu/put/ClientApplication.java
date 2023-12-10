package edu.put;

import edu.put.backend.BackendConfig;
import edu.put.backend.BackendSession;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Slf4j
public class ClientApplication {

    public static void main(String[] args) throws Exception {
        var config = new BackendConfig();
        var backendSession = new BackendSession(config.getContactPoint(), config.getKeyspace());
        var session = backendSession.getSession();
        for (int i = 0; i < 100; i++) {
            String query = String.format("INSERT INTO client_orders (client_id, creation_time, price, food) VALUES (%s, '%s', %s, '%s')",
                    i,
                    new Date().toInstant(),
                    100,
                    "eggs");
            log.info("Running query: {}", query);
            session.execute(query);
        }


        System.exit(0);
    }

}
