package edu.put.database.dao;

import com.datastax.oss.driver.api.core.PagingIterable;
import com.datastax.oss.driver.api.mapper.annotations.Dao;
import com.datastax.oss.driver.api.mapper.annotations.Insert;
import com.datastax.oss.driver.api.mapper.annotations.Query;
import edu.put.database.entities.Delivered;

@Dao
public interface Delivery {
    @Insert
    boolean insert(Delivered order);

    @Query("SELECT * FROM delivery")
    PagingIterable<Delivered> get_all();
}
