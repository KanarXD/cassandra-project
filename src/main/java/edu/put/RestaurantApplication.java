package edu.put;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import edu.put.backend.BackendConfig;
import edu.put.backend.BackendSession;
import edu.put.dto.ClientOrder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RestaurantApplication {

    public static void main(String[] args) throws Exception {
        var config = new BackendConfig();
        var backendSession = new BackendSession(config.getContactPoint(), config.getKeyspace());
        var session = backendSession.getSession();

        ResultSet resultSet = session.execute("SELECT * FROM client_orders;");

        MappingManager manager = new MappingManager(session);
        Mapper<ClientOrder> mapper = manager.mapper(ClientOrder.class);

        for (ClientOrder clientOrder : mapper.map(resultSet).all()) {
            log.info("Client order: {}", clientOrder);
        }
    }
}
