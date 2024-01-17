package edu.put.database.dao;

import com.datastax.oss.driver.api.core.PagingIterable;
import com.datastax.oss.driver.api.mapper.annotations.Dao;
import com.datastax.oss.driver.api.mapper.annotations.Insert;
import com.datastax.oss.driver.api.mapper.annotations.Select;
import edu.put.database.entities.Restaurant;

@Dao
public interface Restaurants {
    @Insert
    boolean insert(Restaurant restaurant);

    @Select
    PagingIterable<Restaurant> get(String category);
}
