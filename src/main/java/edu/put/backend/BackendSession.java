package edu.put.backend;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.servererrors.QueryValidationException;
import com.datastax.oss.driver.api.core.servererrors.UnavailableException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Getter
public record BackendSession(CqlSession session) {
    public BackendSession(CqlSession session) {
        this.session = session;
        configure_session_cleanup();
    }

    public ResultSet execute(String query) {
        ResultSet results = null;
        try {
            log.trace("Running query: {}", query);
            results = session.execute(query);
        } catch (QueryValidationException error) {
            log.error("""
                            
                    Invalid query: `{}`
                    Error: {}""", query, error.getMessage());
        }
//        catch (NoHostAvailableException error) {
//            log.error("""
//
//                    Cannot perform query: `{}`.
//                    No cassandra hosts are available right now.
//                    Error: {}
//                    """, query, error.getMessage());
//        }
        catch (UnavailableException error) {
            log.warn("""
                                        
                    Cannot perform query `{}`.
                    Required number of cassandra nodes is unavailable.
                    Error: {}
                    """, query, error.getMessage());
        } catch (Exception error) {
            log.warn("""
                                        
                    Exception encountered running query: `{}`.
                    Error: {}
                    """, query, error.getMessage());
        }
        return results;
    }

    private void configure_session_cleanup() {
        Thread finish_session = new Thread(() -> {
            log.info("Closing session");
            session.close();
        });
        Runtime.getRuntime().addShutdownHook(finish_session);
    }

}
