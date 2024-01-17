package edu.put.apps;

import edu.put.backend.Foods;
import edu.put.database.dao.DAO;
import edu.put.database.entities.Order;
import edu.put.database.entities.Ordered;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class ClientApplication extends Thread {
    private static int uninserted = 0;
    private final int id;
    private final DAO mapper;
    private final Random random = new Random();

    @Override
    public void run() {
        try {
            for (int i = 0; i < 100; i++) {
                make_order();
                Thread.sleep(random.nextInt(50));
            }
            log.trace("Client #{} made all their orders.", id);

        } catch (Exception e) {
            log.error("Client #{} failed: {}", id, e.getMessage());
        }
    }

    public int failure_count() {
        return uninserted;
    }

    private void make_order() {
        var category = Foods.categories().get(random.nextInt(Foods.categories().size()));
        var food = Foods.variants(category).get(random.nextInt(Foods.variants(category).size()));
        var order_id = UUID.randomUUID().toString();
        var order = new Order(order_id, category, food, id);
        var restaurants = mapper.restaurants().get(category).all().stream().toList();
        if (restaurants.isEmpty()) {
            log.warn("Ordered `{}` couldn't be made.", order);
            synchronized (this) {
                uninserted += 1;
            }
            return;
        }
        var restaurant = restaurants.get(random.nextInt(restaurants.size()));

        var ordered = new Ordered(restaurant.restaurant_id(), Instant.now(), order_id, order);

        if (!mapper.orders().insert(ordered)) {
            log.warn("Ordered `{}` couldn't be inserted.", order);
            synchronized (this) {
                uninserted += 1;
            }
        }
    }
}
