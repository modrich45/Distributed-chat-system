package org.chatapp.util;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;

@ApplicationScoped
public class Util {
    public void sendMessage(Session session, String message) {
        if (session != null && session.isOpen()) {
            session.getAsyncRemote().sendText(message);
        }
    }
}
