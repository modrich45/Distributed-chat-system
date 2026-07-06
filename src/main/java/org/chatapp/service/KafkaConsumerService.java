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

    @Inject
    RetryService retryService;

    @Inject
    Util util;

    private static final ObjectMapper mapper = new ObjectMapper();

    @Incoming("chat-in")
    public void consume(String message) {


        try {
            LOG.info("RAW MESSAGE FROM KAFKA: " + message);
            var json = mapper.readTree(message);
            String type = json.path("type").asText("CHAT");

            if ("CHAT".equals(type)) {
                if (!json.hasNonNull("to") || !json.hasNonNull("from") || !json.hasNonNull("content")) {
                    LOG.warn("Invalid CHAT payload, missing required fields: " + message);
                    return;
                }

                Long receiverId = json.get("to").asLong();
                String targetServer = redisService.getUserServer(receiverId).join();
                if (targetServer == null) {
                    LOG.warn("User " + receiverId + " is offline. Message will be stored for later delivery.");
                    return;
                }

                if (!serverId.equals(targetServer)) {
                    LOG.info("CHAT for user " + receiverId + " is on server " + targetServer + ". Ignoring.");
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

                    if (response.messageId != null && redisService.isMessageProcessed(response.messageId).join()) {
                        LOG.info("Message with messageId " + response.messageId + " has already been processed. Skipping.");
                        return;
                    }

                    LOG.info("Kafka received CHAT: " + message);
                    util.sendMessage(session, response);
                    if (response.messageId != null) {
                        redisService.markMessageProcessed(response.messageId);
                    }
                } else {
                    LOG.warn("No active websocket session for receiverId " + receiverId + " on server " + serverId);
                }
                return;
            }

            if ("READ_ACK".equals(type)) {
                if (!json.hasNonNull("to") || !json.hasNonNull("messageId") || !json.hasNonNull("readerId")) {
                    LOG.warn("Invalid READ_ACK payload, missing required fields: " + message);
                    return;
                }

                Long receiverId = json.get("to").asLong();
                String targetServer = redisService.getUserServer(receiverId).join();
                if (targetServer == null) {
                    LOG.warn("READ_ACK target user " + receiverId + " is offline.");
                    return;
                }

                if (!serverId.equals(targetServer)) {
                    LOG.info("READ_ACK for user " + receiverId + " is on server " + targetServer + ". Ignoring.");
                    return;
                }

                Session session = sessionManager.getSession(receiverId);
                if (session != null && session.isOpen()) {
                    var payload = mapper.createObjectNode();
                    payload.put("type", "READ_ACK");
                    payload.put("messageId", json.get("messageId").asText());
                    payload.put("readerId", json.get("readerId").asLong());
                    util.sendMessage(session, payload);
                    LOG.info("Delivered READ_ACK for messageId " + json.get("messageId").asText() + " to user " + receiverId);
                } else {
                    LOG.warn("No active websocket session for READ_ACK receiverId " + receiverId + " on server " + serverId);
                }
                return;
            }

            LOG.warn("Unknown Kafka payload type '" + type + "': " + message);
        } catch (Exception e) {
            LOG.error("Failed to consume kafka message: " + message, e);
        }
    }
}
