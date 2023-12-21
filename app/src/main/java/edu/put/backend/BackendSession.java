package edu.put.backend;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class BackendSession {
    private final Session session;

    public BackendSession(Session session) {
        this.session = session;
        setSessionClosing();
    }

    public ResultSet execute(String query) {
        log.info("Running query: {}", query);
        return session.execute(query);
    }

    private void setSessionClosing() {
        Thread closeSession = new Thread(() -> {
            log.info("closing session");
            session.close();
        });
        Runtime.getRuntime().addShutdownHook(closeSession);
    }

}
