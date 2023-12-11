package edu.put;

import com.datastax.driver.core.Cluster;
import edu.put.apps.ClientApplication;
import edu.put.apps.DeliveryApplication;
import edu.put.apps.RestaurantApplication;
import edu.put.backend.BackendConfig;
import edu.put.backend.CassandraSession;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AppRunner {
    private static BackendConfig config;
    private static Cluster cluster;

    public static void main(String[] args) throws InterruptedException {
        config = new BackendConfig();
        cluster = Cluster.builder().addContactPoint(config.getContactPoint()).build();

        var clientAppCount = 2;
        var restaurantAppCount = 2;
        var deliveryAppCount = 2;

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
        for (int i = 0; i < restaurantAppCount; i++) {
            restaurantApps[i].join();
        }
        for (int i = 0; i < deliveryAppCount; i++) {
            deliveryApps[i].join();
        }

        System.exit(0);

    }

    private static CassandraSession getCassandraSession() {
        var session = cluster.connect(config.getKeyspace());
        return new CassandraSession(session);
    }


}
