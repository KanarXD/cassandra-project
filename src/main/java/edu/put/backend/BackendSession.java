package edu.put.backend;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Getter
public class BackendSession {

    private final Session session;

    public BackendSession(String contactPoint) throws BackendException {
        Cluster cluster = Cluster.builder().addContactPoint(contactPoint).build();
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

    }

    public BackendSession(String contactPoint, String keyspace) throws BackendException {
        Cluster cluster = Cluster.builder().addContactPoint(contactPoint).build();
        try {
            session = cluster.connect(keyspace);
            Thread closeSession = new Thread(() -> {
                log.info("closing session");
                session.close();
            });
            Runtime.getRuntime().addShutdownHook(closeSession);
        } catch (Exception e) {
            throw new BackendException("Could not connect to the cluster. " + e.getMessage() + ".", e);
        }
    }

}
