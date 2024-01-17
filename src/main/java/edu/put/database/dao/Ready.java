package edu.put.database.dao;

import com.datastax.oss.driver.api.core.PagingIterable;
import com.datastax.oss.driver.api.mapper.annotations.Dao;
import com.datastax.oss.driver.api.mapper.annotations.Insert;
import com.datastax.oss.driver.api.mapper.annotations.Select;

import java.time.Instant;

@Dao
public interface Ready {
    @Insert
    boolean insert(edu.put.database.entities.Ready order);

    @Select(customWhereClause = "date = :date AND timestamp > :timestamp")
    PagingIterable<edu.put.database.entities.Ready> get(String date, Instant timestamp);
}
