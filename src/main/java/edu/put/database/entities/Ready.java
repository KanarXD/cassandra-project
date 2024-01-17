package edu.put.database.entities;

import com.datastax.oss.driver.api.mapper.annotations.ClusteringColumn;
import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;

import java.time.Instant;

// @formatter:off
@Entity
public record Ready(
        @PartitionKey
        String date,
        @ClusteringColumn
        Instant timestamp,
        String order_id,
        @CqlName("details")
        Order order
) {}
// @formatter:on