package org.upyog.mcp.guard;

import io.lettuce.core.RedisClient;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import org.upyog.mcp.config.McpProperties;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis implementation of {@link SingleUseStore}. {@code SET NX} makes token consumption single-use
 * across MCP replicas.
 */
public class RedisSingleUseStore implements SingleUseStore, AutoCloseable {

    private final RedisClient client;
    private final StatefulRedisConnection<String, String> connection;

    /** Opens a Lettuce connection using {@link McpProperties#getRedis()}. */
    public RedisSingleUseStore(McpProperties properties) {
        this.client = RedisClient.create(properties.getRedis().getUri());
        this.connection = client.connect();
    }

    @Override
    public boolean consumeOnce(String key, Duration ttl) {
        String result = connection.sync().set(key, "1", SetArgs.Builder.nx().px(ttl));
        return "OK".equals(result);
    }

    @Override
    public void saveResult(String key, String json, Duration ttl) {
        connection.sync().set(key, json, SetArgs.Builder.px(ttl));
    }

    @Override
    public Optional<String> findResult(String key) {
        return Optional.ofNullable(connection.sync().get(key));
    }

    @Override
    public boolean writesEnabled() {
        return true;
    }

    /** Closes the Redis connection when the Spring context shuts down. */
    @Override
    public void close() {
        connection.close();
        client.shutdown();
    }
}
