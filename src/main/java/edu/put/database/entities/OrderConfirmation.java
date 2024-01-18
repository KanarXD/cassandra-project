package edu.put.database.entities;

import com.datastax.oss.driver.api.mapper.annotations.ClusteringColumn;
import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;

// @formatter:off
@Entity
@CqlName("confirmed")
public record OrderConfirmation(
        @PartitionKey
        String order_id,

        @ClusteringColumn
        int restaurant_id
) {}
// @formatter:on