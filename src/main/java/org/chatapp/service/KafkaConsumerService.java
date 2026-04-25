package org.chatapp.service;

import org.chatapp.dto.SocketResponse;
import org.chatapp.util.SessionManager;
import org.chatapp.util.Util;
import org.chatapp.webSocket.ChatWebSocket;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.Session;

@ApplicationScoped
public class KafkaConsumerService {

    private static final Logger LOG = Logger.getLogger(KafkaConsumerService.class);

    @Inject 
    SessionManager sessionManager;

    private static final ObjectMapper mapper = new ObjectMapper();

    @Incoming("chat-in")
    public void consume(String message) {
        try {
            LOG.info("RAW MESSAGE FROM KAFKA: " + message);
            // Parse JSON
            var json = mapper.readTree(message);

            Long receiverId = json.get("to").asLong();

            Session session = sessionManager.getSession(receiverId);

            if (session != null && session.isOpen()) {
                SocketResponse response = new SocketResponse();
                response.type = "CHAT";
                response.from = json.get("from").asLong();
                response.to = receiverId;
                response.content = json.get("content").asText();
                response.timestamp = json.get("timestamp").asLong();

                LOG.info("Kafka received: " + message);
                Util.sendMessage(session, response);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
