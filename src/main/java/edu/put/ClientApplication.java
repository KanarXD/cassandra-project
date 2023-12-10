package edu.put;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import edu.put.backend.BackendConfig;
import edu.put.backend.BackendException;
import edu.put.backend.BackendSession;
import edu.put.dto.ClientOrder;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Slf4j
public class ClientApplication {

    public static void main(String[] args) throws BackendException {
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


        ResultSet resultSet = session.execute("SELECT * FROM client_orders;");

        MappingManager manager = new MappingManager(session);
        Mapper<ClientOrder> mapper = manager.mapper(ClientOrder.class);

        for (ClientOrder clientOrder : mapper.map(resultSet).all()) {
            log.info("Client order: {}", clientOrder);
        }
        System.exit(0);
    }

}
