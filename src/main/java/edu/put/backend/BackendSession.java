package edu.put.backend;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Getter
public record BackendSession(Session session) {
    public BackendSession(Session session) {
        this.session = session;
        setSessionClosing();
    }

    public ResultSet execute(String query) {
        log.trace("Running query: {}", query);
        return session.execute(query);
    }

    private void setSessionClosing() {
        Thread closeSession = new Thread(() -> {
            log.info("Closing session");
            session.close();
        });
        Runtime.getRuntime().addShutdownHook(closeSession);
    }

}
