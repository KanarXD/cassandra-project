package edu.put.database.dao;

import com.datastax.oss.driver.api.core.PagingIterable;
import com.datastax.oss.driver.api.mapper.annotations.Dao;
import com.datastax.oss.driver.api.mapper.annotations.Insert;
import com.datastax.oss.driver.api.mapper.annotations.Select;
import edu.put.database.entities.Confirmation;

@Dao
public interface Confirm {
    @Insert
    boolean insert(Confirmation order);

    @Select(customWhereClause = "order_id = :order_id")
    PagingIterable<Confirmation> get(String order_id);
}
