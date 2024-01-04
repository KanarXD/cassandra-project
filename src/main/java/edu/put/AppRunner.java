package edu.put;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.mapping.MappingManager;
import edu.put.apps.ClientApplication;
import edu.put.apps.DeliveryApplication;
import edu.put.apps.InitApplication;
import edu.put.apps.RestaurantApplication;
import edu.put.backend.BackendConfig;
import edu.put.backend.BackendSession;
import edu.put.dto.ReadyOrder;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;

@Slf4j
public class AppRunner {
    private static BackendConfig config;
    private static Cluster cluster;

    public static void main(String[] args) throws InterruptedException {
        InitApplication.main(args);
        config = new BackendConfig();
        cluster = Cluster.builder().addContactPoint(config.getContactPoint()).build();

        var clientAppCount = 5;
        var restaurantAppCount = 5;
        var deliveryAppCount = 5;

        var clientApps = new ClientApplication[clientAppCount];
        var restaurantApps = new RestaurantApplication[clientAppCount];
        var deliveryApps = new DeliveryApplication[clientAppCount];

        for (int i = 0; i < clientAppCount; i++) {
            var clientApplication = new ClientApplication(i, getCassandraSession());
            clientApplication.start();
            clientApps[i] = clientApplication;
        }

        for (int i = 0; i < restaurantAppCount; i++) {
            var restaurantApplication = new RestaurantApplication(i, getCassandraSession());
            restaurantApplication.start();
            restaurantApps[i] = restaurantApplication;
        }

        for (int i = 0; i < deliveryAppCount; i++) {
            var deliveryApplication = new DeliveryApplication(i, getCassandraSession());
            deliveryApplication.start();
            deliveryApps[i] = deliveryApplication;
        }

        for (int i = 0; i < clientAppCount; i++) {
            clientApps[i].join();
        }
        log.warn("Killing threads prepare");
        Thread.sleep(20000);
        log.warn("Killing threads");

        for (int i = 0; i < restaurantAppCount; i++) {
            restaurantApps[i].interrupt();
        }
        for (int i = 0; i < deliveryAppCount; i++) {
            deliveryApps[i].interrupt();
        }

        for (int i = 0; i < restaurantAppCount; i++) {
            restaurantApps[i].join();
        }
        for (int i = 0; i < deliveryAppCount; i++) {
            deliveryApps[i].join();
        }

        showStatistics();

        System.exit(0);
    }

    private static void showStatistics() {
        var query = "SELECT * FROM ready_orders;";
        var session = getCassandraSession();
        var mappingManager = new MappingManager(session.getSession());

        var resultSet = session.execute(query);
        var mapper = mappingManager.mapper(ReadyOrder.class);
        var readyOrders = mapper.map(resultSet)
                .all()
                .stream()
                .map(ReadyOrder::getInfo)
                .toList();

        var readyOrdersSet = new HashSet<>(readyOrders);

        log.info("readyOrders size: {}", readyOrders.size());
        log.info("readyOrdersSet size: {}", readyOrdersSet.size());
    }

    private static BackendSession getCassandraSession() {
        var session = cluster.connect(config.getKeyspace());
        return new BackendSession(session);
    }
}
