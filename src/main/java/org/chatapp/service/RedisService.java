package org.chatapp.service;

import java.util.concurrent.CompletableFuture;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RedisService {

    private final ValueCommands<String, String> commands;
    private final KeyCommands<String> keyCommands;

    public RedisService(RedisDataSource ds) {
        this.commands = ds.value(String.class);
        this.keyCommands = ds.key();
    }

    public void setUserOnline(Long userId) {
        CompletableFuture.runAsync(() -> {
            commands.setex("user:" + userId, 3600, "ONLINE");
        });
    }

    public void setUserOffline(Long userId) {
        CompletableFuture.runAsync(() -> {
            keyCommands.del("user:" + userId);
        });
    }

    public CompletableFuture<Boolean> isUserOnline(Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            String status = commands.get("user:" + userId);
            return "ONLINE".equals(status);
        });
    }
}
