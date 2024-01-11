package edu.put.apps;

import com.datastax.driver.mapping.MappingManager;
import edu.put.backend.BackendSession;
import edu.put.backend.Common;
import edu.put.dto.ClientOrder;
import edu.put.records.Confirmation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class ClientApplication extends Thread {
    private final int clientId;
    private final BackendSession session;
    private final MappingManager manager;
    private List<ClientOrder> orders = new ArrayList<>();
    private List<ClientOrder> confirmed_orders = new ArrayList<>();
    private Random random = new Random();

    @Override
    public void run() {
        try {
            for (int i = 0; i < 100; i++) {
                make_order();
                confirm_orders();
                Thread.sleep(random.nextInt(50));
            }

            while (!orders.isEmpty()) {
                log.debug("Client is only confirming");
                confirm_orders();
            }

            log.warn("Confirmed {} orders", confirmed_orders.size());
        } catch (Exception e) {
            log.error("Thread: {}, error: {}", clientId, e.getMessage());
        }
    }

    private void make_order() {
        var foods = Common.foodMap;
        var category = foods.keySet().stream().toList().get(random.nextInt(foods.keySet().size()));
        var food = foods.get(category).get(random.nextInt(foods.get(category).size()));
        var order_id = UUID.randomUUID().toString();
        var order = new ClientOrder(category, new Date(), order_id, clientId, food);
        var query = String.format("INSERT INTO client_orders (food_category, creation_time, user_id, food, order_id) VALUES ('%s', '%s', %s, '%s', '%s')", order.getCategory(), order.getCreationTime().toInstant(), order.getUserId(), order.getFood(), order.getOrderId());

        orders.add(order);
        log.trace("Running query: {}", query);
        session.execute(query);
    }

    private void confirm_orders() {
        var mapper = manager.mapper(Confirmation.class);
        List<ClientOrder> to_remove = new ArrayList<>();

        for (var order : orders) {
            var query = String.format("SELECT * FROM order_confirmation WHERE order_id = '%s'", order.getOrderId());
            var results = session.execute(query);
            var confirmations = mapper.map(results).all().stream().map(Confirmation::getRestaurantId).toList();
            if (!confirmations.isEmpty()) {
                to_remove.add(order);
                confirmed_orders.add(order);
                var index = random.nextInt(confirmations.size());
                var restaurant = confirmations.get(index);
                session.execute(String.format("INSERT INTO orders_in_progress(restaurant_id, creation_time, order_id, info) VALUES (%d, '%s', '%s', '%s')", restaurant, new Date().toInstant(), order.getOrderId(), order));
            }
        }

        orders.removeIf(to_remove::contains);
        to_remove.clear();
    }
}
