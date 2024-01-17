//package edu.put.apps;
//
//import com.datastax.driver.mapping.MappingManager;
//import edu.put.backend.BackendSession;
//import edu.put.records.orders.Delivered;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
//import java.util.List;
//import java.util.Random;
//
//@Slf4j
//@RequiredArgsConstructor
//public class DeliveryApplication extends Thread {
//    private final int id;
//    private final BackendSession session;
//    private final Random random = new Random();
//    private MappingManager mapping;
//
//    @SuppressWarnings("InfiniteLoopStatement")
//    @Override
//    public void run() {
//        try {
//            mapping = new MappingManager(session.session());
//
//            for (int i = 0; true; i++) {
//                var order = get_order();
//                delete_order(order);
//                create_delivered_order(order, String.format("%d:%d", id, i));
//                log.debug("Delivery Courier #{} | Ordered order #{} is: {}", id, i, order);
//            }
//        } catch (InterruptedException e) {
//            log.info("Delivery Courier #{} received interrupt signal. Shutting down.", id);
//        } catch (Exception e) {
//            log.error("Delivery Courier #{} failed. Error: {}", id, e.getMessage());
//        }
//    }
//
//    private void create_delivered_order(Delivered order, String id) {
//        session.execute(String.format("INSERT INTO ready_orders (id, info) VALUES ('%s', '%s')", id, order));
//    }
//
//    private void delete_order(Delivered orderInProgress) {
//        session.execute(String.format("DELETE FROM orders_in_progress WHERE order_id='%s' AND creation_time='%s'", orderInProgress.getOrderId(), orderInProgress.getCreationTime().toInstant()));
//    }
//
//    private Delivered get_order() throws InterruptedException {
//        while (true) {
//            var orders = get_top_orders(10);
//            var order = orders.get(random.nextInt(orders.size()));
//            var query = String.format("SELECT * FROM orders_in_progress WHERE order_id = '%s';", order.getOrderId());
//            if (!session.execute(query).all().isEmpty()) {
//                return order;
//            }
//        }
//    }
//
//    @SuppressWarnings({"SameParameterValue"})
//    private List<Delivered> get_top_orders(int limit) throws InterruptedException {
//        var mapper = mapping.mapper(Delivered.class);
//        List<Delivered> orders = List.of();
//
//        while (orders.isEmpty()) {
//            var results = session.execute(String.format("SELECT * FROM orders_in_progress LIMIT %d;", limit));
//            orders = mapper.map(results).all();
//
//            if (orders.isEmpty() && Thread.currentThread().isInterrupted()) {
//                // Thread was interrupted, so no new orders are coming and we should finish.
//                throw new InterruptedException();
//            }
//        }
//        return orders;
//    }
//}
