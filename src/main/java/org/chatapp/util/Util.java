package org.chatapp.util;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;

@ApplicationScoped
public class Util {

    private static final ObjectMapper mapper = new ObjectMapper(); 

    private static final Logger LOG = Logger.getLogger(Util.class);

    public static void sendMessage(Session session, Object payload) {
        if (session != null && session.isOpen()) {
            try {
                String json = mapper.writeValueAsString(payload);
                LOG.info("Sending message to user " + session.getId() + ": " + json);
                session.getAsyncRemote().sendText(json);
            } catch (Exception e) {
                LOG.error("Failed to send message: " + e.getMessage(), e);
            }
        }
    }
}
