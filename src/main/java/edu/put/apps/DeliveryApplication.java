package edu.put.apps;

import com.datastax.oss.driver.api.core.NoNodeAvailableException;
import edu.put.database.dao.DAO;
import edu.put.database.entities.DeliveryConfirmation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Random;

@Slf4j
@RequiredArgsConstructor
public class DeliveryApplication extends Thread {
    private final int id;
    private final DAO mapper;
    private final Random random = new Random();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Can be changed to some more sane value, like yesterday at midnight.
    private Instant last_timestamp = Instant.ofEpochSecond(0);

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                ask_for_confirmation();
                Thread.sleep(random.nextInt(500));
            }
        } catch (InterruptedException e) {
            log.info("Delivery Courier #{} received interrupt signal. Shutting down.", id);
        } catch (Exception e) {
            log.error("Delivery Courier #{} failed. Error: {}", id, e.getMessage());
        }
    }

    private void ask_for_confirmation() {
        try {
            var date = formatter.format(LocalDate.now());
            var ready = mapper.ready().get(date, last_timestamp.minus(1, ChronoUnit.MINUTES)).all();

            for (var order : ready) {
                mapper.confirm_delivery().insert(new DeliveryConfirmation(order.order_id(), id, order.order()));
                last_timestamp = order.timestamp();
            }
        } catch (NoNodeAvailableException error) {
            log.warn("Couldn't request delivery. Cause: {}", String.join("\n", Arrays.stream(error.getStackTrace()).map(Object::toString).toList()));
        }
    }
}
