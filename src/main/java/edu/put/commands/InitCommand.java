package edu.put.commands;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.datastax.oss.driver.api.core.CqlSession;
import edu.put.database.config.Config;
import edu.put.database.config.Driver;
import edu.put.database.config.Replication;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

@Slf4j
@CommandLine.Command(name = "init")
public class InitCommand implements Runnable {
    @CommandLine.Option(names = {"-k", "--keyspace"})
    private String keyspace;
    @CommandLine.Option(names = {"-s", "--replication-strategy"})
    private String replication_strategy;
    // Fucking important! Type of replication_factor MUST BE `Integer`, not `int`
    // because uninitialized `int` defaults to 0, overrides value from config file
    // and crashes database 'cause there are no replicas.
    @CommandLine.Option(names = {"-f", "--replication-factor"})
    private Integer replication_factor;
    @CommandLine.Option(names = "--contact-point")
    private String contact_point;

    @CommandLine.Option(names = {"-v", "--verbose"})
    private boolean[] verbosity;

    @Override
    public void run() {
        configure_logging();

        log.info("Configuring database.");
        var config = Config.load("config.properties")
                .with_contact_point(contact_point)
                .with_keyspace(keyspace)
                .with_replication(Replication.standard().with_strategy(replication_strategy).with_factor(replication_factor));
        log.info("Loaded configuration: {}", config);
        try (var session = CqlSession.builder().withConfigLoader(Driver.setup()).build()) {
            // Create keyspace in database.
            log.trace("Cleaning up keyspace.");
            session.execute(String.format("DROP KEYSPACE IF EXISTS %s;", config.keyspace()));
            log.trace("Creating keyspace `{}`", config.keyspace());
            session.execute(String.format("CREATE KEYSPACE IF NOT EXISTS %s WITH replication = {'class': '%s', 'replication_factor': %d };", config.keyspace(), config.replication().strategy(), config.replication().factor()));
            log.trace("Keyspace `{}` created.", config.keyspace());
        } catch (Exception error) {
            log.error("Error occurred when creating keyspace: {}", error.getMessage());
        }

        try (var session = CqlSession.builder().withConfigLoader(Driver.setup()).withKeyspace(config.keyspace()).build()) {
            // Cleanup tables.
            log.trace("Cleaning up.");
            session.execute("DROP TABLE IF EXISTS restaurants;");
            session.execute("DROP TABLE IF EXISTS orders;");
            session.execute("DROP TABLE IF EXISTS confirmed;");
            session.execute("DROP TABLE IF EXISTS ready;");
            session.execute("DROP TABLE IF EXISTS delivery_confirmation;");
            session.execute("DROP TABLE IF EXISTS delivery;");
            session.execute("DROP TYPE IF EXISTS CLIENT_ORDER;");

            log.trace("Defining custom types.");
            session.execute("""
                    CREATE TYPE IF NOT EXISTS CLIENT_ORDER (
                        id VARCHAR,
                        category VARCHAR,
                        variant VARCHAR,
                        client INT
                    );
                    """);

            // Create required tables.
            log.trace("Recreating tables.");
            session.execute("""
                    CREATE TABLE restaurants (
                        category VARCHAR,
                        restaurant_id INT,
                        PRIMARY KEY (category, restaurant_id)
                    );
                    """);
            session.execute("""
                    CREATE TABLE orders (
                        restaurant_id INT,
                        timestamp TIMESTAMP,
                        order_id VARCHAR,
                        details CLIENT_ORDER,
                        PRIMARY KEY (restaurant_id, timestamp, order_id)
                    )
                    WITH CLUSTERING ORDER BY (timestamp DESC, order_id ASC);
                    """);

            session.execute("""
                    CREATE TABLE confirmed (
                        order_id VARCHAR,
                        restaurant_id INT,
                        PRIMARY KEY (order_id, restaurant_id)
                    )
                    WITH CLUSTERING ORDER BY (restaurant_id ASC);
                    """);

            session.execute("""
                    CREATE TABLE ready (
                        date VARCHAR,
                        timestamp TIMESTAMP,
                        order_id VARCHAR,
                        details CLIENT_ORDER,
                        PRIMARY KEY (date, timestamp, order_id)
                    )
                    WITH gc_grace_seconds = 0
                    AND CLUSTERING ORDER BY (timestamp ASC, order_id ASC);
                    """);

            session.execute("""
                    CREATE TABLE delivery_confirmation (
                        order_id VARCHAR,
                        delivery_id INT,
                        details CLIENT_ORDER,
                        PRIMARY KEY (order_id, delivery_id)
                    )
                    WITH default_time_to_live = 10
                    AND CLUSTERING ORDER BY (delivery_id ASC);
                    """);

            session.execute("""
                    CREATE TABLE delivery (
                        order_id VARCHAR,
                        delivery_id INT,
                        details CLIENT_ORDER,
                        PRIMARY KEY (order_id, delivery_id)
                    )
                    WITH CLUSTERING ORDER BY (delivery_id ASC);
                    """);

            log.info("Database configured.");
        } catch (Exception error) {
            log.error("Error encountered when setting up database: {}", error.getMessage());
        }
    }

    void configure_logging() {
        var level = switch (verbosity == null ? 0 : verbosity.length) {
            case 0 -> Level.ERROR;
            case 1 -> Level.WARN;
            case 2 -> Level.INFO;
            case 3 -> Level.DEBUG;
            case 4 -> Level.TRACE;
            default -> Level.ALL;
        };

        Logger logger = (Logger) LoggerFactory.getLogger(InitCommand.class);
        logger.setLevel(level);
    }
}
