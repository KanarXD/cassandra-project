package edu.put;

import com.datastax.driver.core.ResultSet;
import edu.put.backend.BackendException;
import edu.put.backend.BackendSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

@Slf4j
public class Main {

    public static void main(String[] args) throws BackendException {
        Logger.getRootLogger().setLevel(Level.INFO);
        log.info("start");
        var cassandraConfig = new CassandraConfig();
        var backendSession = new BackendSession(cassandraConfig);
        var session = backendSession.getSession();
        session.execute("INSERT INTO test1 (key, value) VALUES (0, 'abc');");

        ResultSet result = session.execute("SELECT * FROM test1");

        log.info(result.toString());

        log.info("end");
    }
}
