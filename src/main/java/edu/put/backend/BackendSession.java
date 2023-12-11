package edu.put.backend;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Getter
public class BackendSession {
    private final Cluster cluster;

    private final Session session;

    public BackendSession(String contactPoint) throws BackendException {
        cluster = Cluster.builder().addContactPoint(contactPoint).build();
        try {
            session = cluster.connect();
            setSessionClosing();
        } catch (Exception e) {
            throw new BackendException("Could not connect to the cluster. " + e.getMessage() + ".", e);
        }

    }

    public BackendSession(String contactPoint, String keyspace) throws BackendException {
        cluster = Cluster.builder().addContactPoint(contactPoint).build();
        try {
            session = cluster.connect(keyspace);
            setSessionClosing();
        } catch (Exception e) {
            throw new BackendException("Could not connect to the cluster. " + e.getMessage() + ".", e);
        }
    }

    public ResultSet execute(String query) {
        log.info("Running query: {}", query);
        return session.execute(query);
    }

    private void setSessionClosing() {
        Thread closeSession = new Thread(() -> {
            log.info("closing session");
            session.close();
            cluster.close();
        });
        Runtime.getRuntime().addShutdownHook(closeSession);
    }

}
