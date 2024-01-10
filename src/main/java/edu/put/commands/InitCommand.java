package edu.put.commands;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.datastax.driver.core.Cluster;
import edu.put.records.Config;
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
    @CommandLine.Option(names = {"-f", "--replication-factor"})
    private int replication_factor;
    @CommandLine.Option(names = "--contact-point")
    private String contact_point;

    @CommandLine.Option(names = {"-v", "--verbose"})
    private boolean[] verbosity;

    @Override
    public void run() {
        configure_logging();

        log.info("Configuring database.");
        var config = Config.load("config.properties").modify(contact_point, keyspace, replication_strategy, replication_factor);

        try (var cluster = Cluster.builder().addContactPoint(config.contact_point()).build()) {
            var session = cluster.newSession();

            // Create keyspace in database.
            log.trace("Creating keyspace `{}`", config.keyspace());
            session.execute(String.format("CREATE KEYSPACE IF NOT EXISTS %s WITH replication = {'class': '%s', 'replication_factor': %d };", config.keyspace(), config.replication().strategy(), config.replication().factor()));
            log.trace("Keyspace `{}` created.", config.keyspace());

            // Enter keyspace.
            log.trace("Entering keyspace `{}`", config.keyspace());
            session.execute(String.format("USE %s;", config.keyspace()));

            // Cleanup tables.
            log.trace("Cleaning up tables.");
            session.execute("DROP TABLE IF EXISTS client_orders;");
            session.execute("DROP TABLE IF EXISTS order_confirmation");
            session.execute("DROP TABLE IF EXISTS orders_in_progress;");
            session.execute("DROP TABLE IF EXISTS ready_orders;");

            // Create required tables.
            log.trace("Recreating tables.");
            session.execute("""
                    CREATE TABLE client_orders (
                        food_category VARCHAR,
                        creation_time TIMESTAMP,
                        order_id VARCHAR,
                        user_id INT,
                        food VARCHAR,
                        PRIMARY KEY (food_category, creation_time, order_id)
                    )
                    WITH CLUSTERING ORDER BY (creation_time DESC);
                    """);

            session.execute("""
                    CREATE TABLE order_confirmation (
                        order_id VARCHAR,
                        restaurant_id INT,
                        info VARCHAR,
                        PRIMARY KEY (order_id, restaurant_id)
                    )
                    WITH CLUSTERING ORDER BY (restaurant_id ASC);
                    """);

            session.execute("""
                    CREATE TABLE orders_in_progress (
                        restaurant_id INT,
                        creation_time TIMESTAMP,
                        order_id VARCHAR,
                        info VARCHAR,
                        PRIMARY KEY (restaurant_id, creation_time)
                    )
                    WITH CLUSTERING ORDER BY (creation_time DESC);
                    """);

            session.execute("""
                    CREATE TABLE ready_orders (
                        id VARCHAR,
                        creation_time TIMESTAMP,
                        info VARCHAR,
                        PRIMARY KEY (id)
                    );
                    """);

            log.info("Database configured.");
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
