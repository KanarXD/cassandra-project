# Cassandra-projekt

## Run cassandra

```bash
docker-compose up -d
```

To run initialization run app with args

```bash
<main class invocation> init [(-k, --keyspace) NAME] [(-s, --replication-strategy) STRATEGY] [(-f, --replication-factor) FACTOR] [--contact-point ADDRESS] [-v, --verbose]
```

To run client-restaurant-delivery system run app with args

```bash
<main class invocation> run [(-k, --keyspace) NAME] [(-c, --clients) COUNT] [(-r, --restaurants) COUNT] [(-d, --delivery-couriers) COUNT] [-v, --verbose]
```

## App flow

