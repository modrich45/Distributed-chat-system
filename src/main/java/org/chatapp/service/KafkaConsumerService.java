package org.chatapp.service;

import org.chatapp.dto.SocketResponse;
import org.chatapp.util.SessionManager;
import org.chatapp.util.Util;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.Session;

@ApplicationScoped
public class KafkaConsumerService {

    @Inject
    RedisService redisService;

    @ConfigProperty(name = "server.id")
    String serverId;

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

            if (!json.hasNonNull("to") || !json.hasNonNull("from") || !json.hasNonNull("content")) {
                LOG.warn("Invalid Kafka payload, missing required fields: " + message);
                return;
            }

            Long receiverId = json.get("to").asLong();

            String targetServer = redisService.getUserServer(receiverId).join();
            if (targetServer == null) {
                LOG.warn("User " + receiverId + " is offline. Message will be stored for later delivery.");
                return;
            }

            if(!serverId.equals(targetServer)) {
                LOG.info("Message for user " + receiverId + " is on server " + targetServer + ". Ignoring.");
                return;
            }

            Session session = sessionManager.getSession(receiverId);

            if (session != null && session.isOpen()) {
                SocketResponse response = new SocketResponse();
                response.messageId = json.path("messageId").asText(null);
                response.type = "CHAT";
                response.from = json.get("from").asLong();
                response.to = receiverId;
                response.content = json.get("content").asText();
                response.timestamp = json.path("timestamp").asLong(System.currentTimeMillis());

                if(redisService.isMessageProcessed(response.messageId).join()) {
                    LOG.info("Message with messageId " + response.messageId + " has already been processed. Skipping.");
                    return;
                }

                LOG.info("Kafka received: " + message);
                Util.sendMessage(session, response);
                redisService.markMessageProcessed(response.messageId);
            } else {
                LOG.warn("No active websocket session for receiverId " + receiverId + " on server " + serverId);
            }

        } catch (Exception e) {
            LOG.error("Failed to consume kafka message: " + message, e);
        }
    }
}
