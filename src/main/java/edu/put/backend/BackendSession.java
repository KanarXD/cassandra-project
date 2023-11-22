package edu.put.backend;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import edu.put.CassandraConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;


@Slf4j
@Getter
public class BackendSession {
    private static final List<String> TABLES = List.of(
            """
                    CREATE TABLE IF NOT EXISTS test1 (
                    key INT PRIMARY KEY,
                    value VARCHAR
                    );""",
            """
                    CREATE TABLE IF NOT EXISTS test2 (
                    key INT PRIMARY KEY,
                    value VARCHAR
                    );"""
    );

    private final Session session;

    public BackendSession(CassandraConfig config) throws BackendException {
        Cluster cluster = Cluster.builder().addContactPoint(config.getContactPoint()).build();
        try {
            session = cluster.connect();
            Thread closeSession = new Thread(() -> {
                log.info("closing session");
                session.close();
            });
            Runtime.getRuntime().addShutdownHook(closeSession);
        } catch (Exception e) {
            throw new BackendException("Could not connect to the cluster. " + e.getMessage() + ".", e);
        }
        createKeyspace(config);
        useKeyspace(config.getKeyspace());
        createTables();
    }

    private void useKeyspace(String keyspace) {
        String query = String.format("USE %s;", keyspace);
        log.info("Use keyspace: " + query);
        session.execute(query);
    }

    private void createTables() {
        for (String table : TABLES) {
            log.info("Creating table:\n{}", table);
            session.execute(table);
        }
    }

    public void createKeyspace(CassandraConfig config) {
        String query = String.format("CREATE KEYSPACE IF NOT EXISTS %s WITH replication = {'class': '%s', 'replication_factor': %d };",
                config.getKeyspace(), config.getReplicationStrategy(), config.getReplicationFactor());
        log.info("Creating keyspace: " + query);
        session.execute(query);
    }

}
