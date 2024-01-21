package edu.put.commands;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.datastax.oss.driver.api.core.CqlSession;
import edu.put.Main;
import edu.put.apps.ClientApplication;
import edu.put.apps.DeliveryApplication;
import edu.put.apps.RestaurantApplication;
import edu.put.backend.BackendSession;
import edu.put.database.config.Config;
import edu.put.database.config.Replication;
import edu.put.database.dao.DAOBuilder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.List;

@Slf4j
@CommandLine.Command(name = "run")
public class RunCommand implements Runnable {
    @CommandLine.Option(names = {"-k", "--keyspace"})
    String keyspace;
    @CommandLine.Option(names = {"-c", "--clients"}, defaultValue = "2")
    int clients;
    @CommandLine.Option(names = {"-r", "--restaurants"}, defaultValue = "2")
    int restaurants;
    @CommandLine.Option(names = {"-d", "--delivery-couriers"}, defaultValue = "2")
    int deliveries;
    @CommandLine.Option(names = {"-v", "--verbose"})
    boolean[] verbosity;

    private static void show_stats(BackendSession session) {
        // TODO: Gather stats and do bandwidth test.
//        var mapper = new MappingManager(session.session()).mapper(Ready.class);
//
//        var results = session.execute("SELECT * FROM ready_orders;");
//        var orders = mapper.map(results).all().stream().map(Ready::getInfo).toList();
//
//        var unique_orders = new HashSet<>(orders);
//
//        log.info("""
//
//                Orders delivered: {}
//                Unique orders delivered: {},
//                Error rate: {}""",
//                orders.size(), unique_orders.size(), String.format("%.04f", 1.0f - ((float) unique_orders.size() / (float) orders.size())));
    }

    @Override
    public void run() {
        configure_logging();

        var config = Config.load("config.properties").with_keyspace(keyspace);
        try (var session = CqlSession.builder().withLocalDatacenter("datacenter1").withKeyspace(config.keyspace()).build()) {
            var mapper = new DAOBuilder(session).build();

            var client_apps = new ClientApplication[clients];
            var restaurant_apps = new RestaurantApplication[restaurants];
            var delivery_apps = new DeliveryApplication[deliveries];

            log.info("Starting restaurant applications.");
            for (int id = 0; id < restaurants; id++) {
                var app = new RestaurantApplication(id, mapper, List.of("pizza", "kebab", "drink")).with_retry_offset(15000);
                app.start();
                restaurant_apps[id] = app;
            }

            log.info("Starting client applications.");
            for (int id = 0; id < clients; id++) {
                var app = new ClientApplication(id, mapper).with_retry_offset(15000);
                app.start();
                client_apps[id] = app;
            }

            log.info("Starting delivery applications.");
            for (int id = 0; id < deliveries; id++) {
                var app = new DeliveryApplication(id, mapper);
                app.start();
                delivery_apps[id] = app;
            }

            log.info("Joining client applications.");
            for (var app : client_apps) {
                try {
                    app.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            log.info("Sending finish signal to restaurant applications.");
            for (var app : restaurant_apps) {
                try {
                    app.interrupt();
                } catch (Exception error) {
                    throw new RuntimeException(error);
                }
            }

            log.info("Joining restaurant applications.");
            for (var app : restaurant_apps) {
                try {
                    app.join();
                } catch (Exception error) {
                    throw new RuntimeException(error);
                }
            }

            log.info("Sending finish signal to delivery applications.");
            for (var app : delivery_apps) {
                try {
                    app.interrupt();
                } catch (Exception error) {
                    throw new RuntimeException(error);
                }
            }

            log.info("Joining delivery applications.");
            for (var app : delivery_apps) {
                try {
                    app.join();
                } catch (Exception error) {
                    throw new RuntimeException(error);
                }
            }

        }
    }

    void configure_logging() {
        var level = switch (verbosity.length) {
            case 0 -> Level.ERROR;
            case 1 -> Level.WARN;
            case 2 -> Level.INFO;
            case 3 -> Level.DEBUG;
            case 4 -> Level.TRACE;
            default -> Level.ALL;
        };

        // @formatter:off
        var loggers = List.of(
                ClientApplication.class,
                DeliveryApplication.class,
                RestaurantApplication.class,
                BackendSession.class,
                InitCommand.class,
                RunCommand.class,
//                Ordered.class,
//                Delivered.class,
//                Ready.class,
                Config.class,
                Replication.class,
                Main.class
        );
        // @formatter:on

        for (var clazz : loggers) {
            Logger logger = (Logger) LoggerFactory.getLogger(clazz);
            logger.setLevel(level);
        }
    }
}
