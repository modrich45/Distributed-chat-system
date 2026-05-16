package org.chatapp.service;

import java.util.concurrent.CompletableFuture;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RedisService {

    @ConfigProperty(name = "server.id")
    String serverId;

    @Inject
    ManagedExecutor managedExecutor;

    private final ValueCommands<String, String> commands;
    private final KeyCommands<String> keyCommands;

    public RedisService(RedisDataSource ds) {
        this.commands = ds.value(String.class);
        this.keyCommands = ds.key();
    }

    public void setUserOnline(Long userId) {
        managedExecutor.runAsync(() -> {
            commands.setex("user:" + userId, 3600, serverId);
        });
    }

    public void setUserOffline(Long userId) {
        managedExecutor.runAsync(() -> {
            keyCommands.del("user:" + userId);
        });
    }

    public CompletableFuture<Boolean> isUserOnline(Long userId) {
        return managedExecutor.supplyAsync(() -> {
            String assignedServer = commands.get("user:" + userId);
            return assignedServer != null && !assignedServer.isBlank();
        });
    }

    public CompletableFuture<String> getUserServer(Long userId) {
        return managedExecutor.supplyAsync(() -> {
            return commands.get("user:" + userId);
        });
    }

    public CompletableFuture<Boolean> isMessageProcessed(String messageId) {
        return managedExecutor.supplyAsync(() -> {
            String result = commands.get("processed:" + messageId);
            return result != null && !result.isBlank();
        });
    }

    public void markMessageProcessed(String messageId) {

        commands.setex(
                "processed:" + messageId,
                3600,
                "true");
    }

    public boolean isRateLimited(Long userId){
        String key="rate:" + userId;
        String current = commands.get(key);
        if(current == null){
            commands.setex(key, 60, "1");
            return false;
        } else {
            int count = Integer.parseInt(current);
            if(count >= 10){
                return true;
            } else {
                commands.incr(key);
                return false;
            }
        }
    }
    
}
