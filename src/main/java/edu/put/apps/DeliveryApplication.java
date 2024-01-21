package edu.put.apps;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.NoNodeAvailableException;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchStatementBuilder;
import com.datastax.oss.driver.api.core.cql.BatchType;
import edu.put.database.dao.DAO;
import edu.put.database.entities.DeliveryConfirmation;
import edu.put.database.entities.Ready;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Slf4j
@RequiredArgsConstructor
public class DeliveryApplication extends Thread {
    private static final Object mutex = new Object();
    // Statistics.
    public static int total_writes = 0;
    public static int missed_writes = 0;
    public static int missed_reads = 0;

    private final int id;
    private final DAO mapper;
    private final CqlSession session;
    private final Random random = new Random();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Can be changed to some more sane value, like yesterday at midnight.
    private Instant last_timestamp = Instant.ofEpochSecond(0);

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                ask_for_confirmation();
                Thread.sleep(random.nextInt(1000));
            }
        } catch (InterruptedException e) {
            log.info("Delivery Courier #{} received interrupt signal. Shutting down.", id);
        } catch (Exception e) {
            log.error("Delivery Courier #{} failed. Error: {}", id, e.getMessage());
        }
    }

    private void ask_for_confirmation() {
        var ready = getReadies();
        if (ready != null) {
            confirm_delivieries(ready);
        }
    }

    private void confirm_delivieries(List<Ready> ready) {
        try {
//            for (var order : ready) {
//                mapper.confirm_delivery().insert(new DeliveryConfirmation(order.order_id(), id, order.order()));
//                last_timestamp = order.timestamp();
//            }
            BatchStatementBuilder builder = BatchStatement.builder(BatchType.LOGGED);

            for (int i = 0; i < ready.size(); i++) {
                var order = ready.get(i);
                builder.addStatement(mapper.confirm_delivery().save(new DeliveryConfirmation(order.order_id(), id, order.order())));
                last_timestamp = order.timestamp();
                if (i % 30 == 0) {
                    var batch = builder.build();
                    increase_total_writes(batch.size());
                    session.execute(batch);
                    builder = BatchStatement.builder(BatchType.LOGGED);
                }
            }
            var batch = builder.build();
            if (batch.size() > 0) {
                increase_total_writes(batch.size());
                session.execute(batch);
            }
        } catch (NoNodeAvailableException error) {
            log.warn("Couldn't request delivery. Cause: {}", String.join("\n", Arrays.stream(error.getStackTrace()).map(Object::toString).toList()));
            synchronized (mutex) {
                missed_writes++;
            }
        }
    }

    private List<Ready> getReadies() {
        try {
            var date = formatter.format(LocalDate.now());
            return mapper.ready().get(date, last_timestamp.minus(1, ChronoUnit.MINUTES), 100).all();
        } catch (Exception e) {
            synchronized (mutex) {
                missed_reads++;
            }
        }
        return null;
    }

    private void increase_total_writes(int count) {
        synchronized (mutex) {
            total_writes += count;
        }
    }
}
