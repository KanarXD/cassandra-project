package edu.put.backend;

import java.util.List;

public class BackendInitScripts {
    public static final List<String> SCRIPTS = List.of(
            "DROP TABLE IF EXISTS client_orders;",
            "DROP TABLE IF EXISTS prepared_orders;",
            "DROP TABLE IF EXISTS ready_orders;",
            """
                    CREATE TABLE client_orders (
                    food_category VARCHAR,
                    creation_time TIMESTAMP,
                    user_id INT,
                    food VARCHAR,
                    order_id VARCHAR,
                    PRIMARY KEY (food_category, creation_time)
                    ) WITH CLUSTERING ORDER BY (creation_time DESC);""",
            """
                    CREATE TABLE prepared_orders (
                    order_id VARCHAR,
                    creation_time TIMESTAMP,
                    restaurant_id INT,
                    price INT,
                    info VARCHAR,
                    PRIMARY KEY (order_id, creation_time)
                    ) WITH CLUSTERING ORDER BY (creation_time DESC);""",
            """
                    CREATE TABLE ready_orders (
                    id INT,
                    info VARCHAR,
                    PRIMARY KEY (id)
                    );"""
    );

}
