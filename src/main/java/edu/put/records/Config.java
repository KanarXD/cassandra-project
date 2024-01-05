package edu.put.records;

import java.io.IOException;
import java.util.Properties;

public record Config(String contact_point, String keyspace, Replication replication) {
    public static Config load(String filename) throws RuntimeException {
        try {
            Properties config = new Properties();
            config.load(Config.class.getClassLoader().getResourceAsStream(filename));

            var contact_point = config.getProperty("contact_point");
            var keyspace = config.getProperty("keyspace");
            var replication_strategy = config.getProperty("replication_strategy");
            var replication_factor = Integer.parseInt(config.getProperty("replication_factor"));

            return new Config(contact_point, keyspace, new Replication(replication_strategy, replication_factor));
        } catch (IOException ex) {
            throw new RuntimeException(String.format("Failed to read %s", filename));
        }
    }

    public Config modify(String contact_point, String keyspace, String replication_strategy, Integer replication_factor) {
        var _contact = contact_point == null ? this.contact_point : contact_point;
        var _keyspace = keyspace == null ? this.keyspace : keyspace;
        var _strategy = replication_strategy == null ? this.replication.strategy() : replication_strategy;
        var _factor = replication_factor == null ? this.replication.factor() : replication_factor;

        return new Config(_contact, _keyspace, new Replication(_strategy, _factor));
    }
}
