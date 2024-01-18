package edu.put.database.dao;

import com.datastax.oss.driver.api.mapper.annotations.Dao;
import com.datastax.oss.driver.api.mapper.annotations.Insert;
import com.datastax.oss.driver.api.mapper.annotations.Select;
import edu.put.database.entities.OrderConfirmation;

@Dao
public interface ConfirmOrder {
    @Select(customWhereClause = "order_id = :order_id")
    OrderConfirmation get(String order_id);

    @Insert
    boolean insert(OrderConfirmation confirmation);
}
