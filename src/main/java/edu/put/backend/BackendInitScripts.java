package edu.put.backend;

import java.util.List;

public class BackendInitScripts {
    public static final List<String> SCRIPTS = List.of(
            "DROP TABLE IF EXISTS client_orders;",
            "DROP TABLE IF EXISTS orders_in_progress;",
            "DROP TABLE IF EXISTS ready_orders;",
            """
                    CREATE TABLE client_orders (
                    foodCategory VARCHAR,
                    creationTime TIMESTAMP,
                    orderId VARCHAR,
                    userId INT,
                    food VARCHAR,
                    PRIMARY KEY (foodCategory, creationTime, orderId)
                    ) WITH CLUSTERING ORDER BY (creationTime DESC);""",
            """
                    CREATE TABLE orders_in_progress (
                    orderId VARCHAR,
                    creationTime TIMESTAMP,
                    status VARCHAR,
                    restaurantId INT,
                    info VARCHAR,
                    PRIMARY KEY (orderId, creationTime)
                    ) WITH CLUSTERING ORDER BY (creationTime DESC);""",
            """
                    CREATE TABLE ready_orders (
                    id VARCHAR,
                    info VARCHAR,
                    PRIMARY KEY (id)
                    );"""
    );

}
