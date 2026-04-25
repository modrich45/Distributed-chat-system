package org.chatapp.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.websocket.Session;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SessionManager {
    private final Map<Long, Session> onlineUsers = new ConcurrentHashMap<>();

    public void addSession(Long userId, Session session) {
        onlineUsers.put(userId, session);
    }

    public void removeSession(Long userId) {
        onlineUsers.remove(userId);
    }

    public Session getSession(Long userId) {
        return onlineUsers.get(userId);
    }
    
}
