package edu.put.database.entities;

import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;

// @formatter:off
@Entity
@CqlName("CLIENT_ORDER")
public record Order(
        String id,
        String category,
        String variant,
        int client
) {}
// @formatter:on