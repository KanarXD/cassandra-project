package edu.put.database.config;

import java.io.IOException;
import java.util.Properties;

public record Config(String datacenter, String contact_point, String keyspace, Replication replication) {
    public static Config load(String filename) throws RuntimeException {
        try {
            Properties config = new Properties();
            config.load(Config.class.getClassLoader().getResourceAsStream(filename));

            var datacenter = config.getProperty("datacenter");
            var contact_point = config.getProperty("contact_point");
            var keyspace = config.getProperty("keyspace");
            var replication_strategy = config.getProperty("replication_strategy");
            var replication_factor = Integer.parseInt(config.getProperty("replication_factor"));

            return new Config(datacenter, contact_point, keyspace, new Replication(replication_strategy, replication_factor));
        } catch (IOException ex) {
            throw new RuntimeException(String.format("Failed to read %s", filename));
        }
    }

    public Config with_datacenter(String datacenter) {
        return new Config(datacenter, this.contact_point, this.keyspace, this.replication);
    }

    public Config with_contact_point(String contact_point) {
        return new Config(this.datacenter, contact_point, this.keyspace, this.replication);
    }

    public Config with_keyspace(String keyspace) {
        return new Config(this.datacenter, this.contact_point, keyspace, this.replication);
    }

    public Config with_replication(Replication replication) {
        return new Config(this.datacenter, this.contact_point, this.keyspace, replication);
    }
}
