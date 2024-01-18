package edu.put.database.dao;

import com.datastax.oss.driver.api.core.PagingIterable;
import com.datastax.oss.driver.api.mapper.annotations.Dao;
import com.datastax.oss.driver.api.mapper.annotations.Insert;
import com.datastax.oss.driver.api.mapper.annotations.Select;
import edu.put.database.entities.DeliveryConfirmation;

@Dao
public interface ConfirmDelivery {
    @Insert
    boolean insert(DeliveryConfirmation order);

    @Select(customWhereClause = "order_id = :order_id")
    PagingIterable<DeliveryConfirmation> get(String order_id);
}
