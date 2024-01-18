package edu.put.database.config;

public record Replication(String strategy, int factor) {
    public static Replication standard() {
        return new Replication("SimpleStrategy", 2);
    }
    public Replication with_strategy(String strategy) {
        return new Replication(strategy == null ? this.strategy : strategy, this.factor);
    }

    public Replication with_factor(Integer factor) {
        return new Replication(this.strategy, factor  == null ? this.factor : factor);
    }
}
