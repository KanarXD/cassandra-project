package edu.put.apps;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import edu.put.backend.BackendConfig;
import lombok.extern.slf4j.Slf4j;

import static edu.put.backend.BackendInitScripts.SCRIPTS;

@Slf4j
public class InitApplication {

    public static void main(String[] args) {
        var config = new BackendConfig();
        var cluster = Cluster.builder().addContactPoint(config.getContactPoint()).build();
        var session = cluster.newSession();

        createKeyspace(session, config);
        useKeyspace(session, config.getKeyspace());
        initScripts(session);

        System.exit(0);
    }

    private static void useKeyspace(Session session, String keyspace) {
        String query = String.format("USE %s;", keyspace);
        log.info("Use keyspace: " + query);
        session.execute(query);
    }

    private static void initScripts(Session session) {
        for (String script : SCRIPTS) {
            log.info("Running script:\n{}", script);
            var preparedStatement = session.prepare(script).setConsistencyLevel(ConsistencyLevel.ALL);
            var boundStatement = new BoundStatement(preparedStatement);
            var result = session.execute(boundStatement);
            log.info(result.toString());
        }
    }

    public static void createKeyspace(Session session, BackendConfig config) {
        String query = String.format("CREATE KEYSPACE IF NOT EXISTS %s WITH replication = {'class': '%s', 'replication_factor': %d };",
                config.getKeyspace(), config.getReplicationStrategy(), config.getReplicationFactor());
        log.info("Creating keyspace: " + query);
        session.execute(query);
    }
}
