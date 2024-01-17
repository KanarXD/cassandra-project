package edu.put.database.dao;

import com.datastax.oss.driver.api.core.PagingIterable;
import com.datastax.oss.driver.api.mapper.annotations.*;
import edu.put.database.entities.Ordered;

import java.time.Instant;

@Dao
public interface Orders {
    @Insert
    boolean insert(Ordered order);

    @Select(customWhereClause = "restaurant_id = :restaurant_id AND timestamp > :timestamp")
    PagingIterable<Ordered> get(int restaurant_id, Instant timestamp);
}
