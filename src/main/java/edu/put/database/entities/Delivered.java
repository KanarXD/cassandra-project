package edu.put.database.entities;

import com.datastax.oss.driver.api.mapper.annotations.ClusteringColumn;
import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;

// @formatter:off
@Entity
@CqlName("delivery")
public record Delivered (
    @PartitionKey
    int delivery_id,
    @ClusteringColumn
    String order_id,
    @CqlName("details")
    Order order
) {}
// @formatter:on