package edu.put.database.dao;

import com.datastax.oss.driver.api.core.PagingIterable;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.BoundStatementBuilder;
import com.datastax.oss.driver.api.mapper.annotations.Dao;
import com.datastax.oss.driver.api.mapper.annotations.Insert;
import com.datastax.oss.driver.api.mapper.annotations.Select;
import edu.put.database.entities.DeliveryConfirmation;

import java.util.List;
import java.util.function.Function;

@Dao
public interface ConfirmDelivery {
    @Insert
    boolean insert(DeliveryConfirmation order);

    @Insert
    BoundStatement save(DeliveryConfirmation order);

    @Select(customWhereClause = "order_id = :order_id")
    PagingIterable<DeliveryConfirmation> get(String order_id);
}
