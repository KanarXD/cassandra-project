package edu.put;


import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Properties;

@Slf4j
@Getter
public class CassandraConfig {
    private static final String PROPERTIES_FILENAME = "config.properties";
    private final String contactPoint;
    private final String keyspace;
    private final String replicationStrategy;
    private final int replicationFactor;

    public CassandraConfig() {
        Properties properties = new Properties();
        try {
            properties.load(Main.class.getClassLoader().getResourceAsStream(PROPERTIES_FILENAME));
            contactPoint = properties.getProperty("contact_point");
            keyspace = properties.getProperty("keyspace");
            replicationStrategy = properties.getProperty("replication_strategy");
            replicationFactor = Integer.parseInt(properties.getProperty("replication_factor"));
        } catch (IOException ex) {
            throw new RuntimeException(String.format("Failed to read %s", PROPERTIES_FILENAME));
        }
    }
}
