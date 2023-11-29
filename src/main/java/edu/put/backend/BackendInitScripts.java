package edu.put.backend;

import java.util.List;

public class BackendInitScripts {
    public static final List<String> SCRIPTS = List.of(
            "DROP TABLE client_orders;",
            "DROP TABLE ready_orders;",
            """
                    CREATE TABLE client_orders (
                    client_id INT,
                    creation_time timestamp,
                    price INT,
                    food VARCHAR,
                    PRIMARY KEY (client_id, creation_time)
                    ) WITH CLUSTERING ORDER BY (creation_time DESC);""",
            """
                    CREATE TABLE ready_orders (
                    client_id INT,
                    creation_time timestamp,
                    restaurant_id INT,
                    price INT,
                    food VARCHAR,
                    PRIMARY KEY (client_id, creation_time)
                    ) WITH CLUSTERING ORDER BY (creation_time DESC);"""
    );

}
