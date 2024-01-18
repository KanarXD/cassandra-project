package edu.put.database.config;

import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.internal.core.config.typesafe.DefaultDriverConfigLoader;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Driver {
    private static Config load_config(String prefix) {
        // Make sure we see the changes when reloading:
        ConfigFactory.invalidateCaches();

        // Every config file in the classpath, without stripping the prefixes
        Config root = ConfigFactory.load();

        // The driver's built-in defaults, under the default prefix in reference.conf:
        Config reference = root.getConfig("datastax-java-driver");

        // Everything under your custom prefix in application.conf:
        Config application = root.getConfig(prefix);

        return application.withFallback(reference);
    }

    public static DriverConfigLoader setup() {
        return new DefaultDriverConfigLoader(() -> load_config("setup"));
    }

    public static DriverConfigLoader stats() {
        return new DefaultDriverConfigLoader(() -> load_config("statistics"));
    }
}
