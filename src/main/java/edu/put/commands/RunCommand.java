package edu.put.commands;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.mapping.MappingManager;
import edu.put.apps.ClientApplication;
import edu.put.apps.DeliveryApplication;
import edu.put.apps.RestaurantApplication;
import edu.put.backend.BackendSession;
import edu.put.dto.ReadyOrder;
import edu.put.records.Config;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.HashSet;

import static org.slf4j.Logger.ROOT_LOGGER_NAME;

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
        var mapper = new MappingManager(session.session()).mapper(ReadyOrder.class);

        var results = session.execute("SELECT * FROM ready_orders;");
        var orders = mapper.map(results).all().stream().map(ReadyOrder::getInfo).toList();

        var unique_orders = new HashSet<>(orders);

        log.info("""
                                
                Orders delivered: {}
                Unique orders delivered: {},
                Error rate: {}""",
                orders.size(), unique_orders.size(), String.format("%.04f", 1.0f - ((float) unique_orders.size() / (float) orders.size())));
    }

    @Override
    public void run() {
        configure_logging();

        var config = Config.load("config.properties").modify(null, keyspace, null, null);
        try (var cluster = Cluster.builder().addContactPoint(config.contact_point()).build()) {
            log.info("Initializing session.");
            var session = new BackendSession(cluster.connect(config.keyspace()));

            var client_apps = new ClientApplication[clients];
            var restaurant_apps = new RestaurantApplication[restaurants];
            var delivery_apps = new DeliveryApplication[deliveries];

            log.info("Starting client applications.");
            for (int id = 0; id < clients; id++) {
                var app = new ClientApplication(id, session);
                app.start();
                client_apps[id] = app;
            }

            log.info("Starting restaurant applications.");
            for (int id = 0; id < restaurants; id++) {
                var app = new RestaurantApplication(id, session);
                app.start();
                restaurant_apps[id] = app;
            }

            log.info("Starting delivery applications.");
            for (int id = 0; id < deliveries; id++) {
                var app = new DeliveryApplication(id, session);
                app.start();
                delivery_apps[id] = app;
            }

            log.info("Waiting for clients to finish.");
            for (int i = 0; i < clients; i++) {
                client_apps[i].join();
            }
            log.info("Clients finished.");

            log.debug("Sending finish signal to restaurants.");

            for (int i = 0; i < restaurants; i++) {
                restaurant_apps[i].interrupt();
            }

            for (int i = 0; i < restaurants; i++) {
                restaurant_apps[i].join();
            }
            log.info("Restaurants finished.");

            for (int i = 0; i < deliveries; i++) {
                delivery_apps[i].interrupt();
            }

            for (int i = 0; i < deliveries; i++) {
                delivery_apps[i].join();
            }
            log.info("Deliveries finished.");

            show_stats(session);
        } catch (InterruptedException e) {
            // TODO: Give some explanation here.
            throw new RuntimeException(e);
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

        Logger logger = (Logger) LoggerFactory.getLogger(ROOT_LOGGER_NAME);
        logger.setLevel(level);
    }
}
