package org.chatapp.webSocket;

import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.chatapp.dto.ChatMessage;
import org.chatapp.dto.SocketResponse;
import org.chatapp.entity.Message;
import org.chatapp.enums.MessageStatus;
import org.chatapp.service.KafkaProducerService;
import org.chatapp.service.MessageService;
import org.chatapp.service.RedisService;
import org.chatapp.util.SessionManager;
import org.chatapp.util.Util;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.enterprise.context.ApplicationScoped;

@ServerEndpoint("/chat/{userId}")
@ApplicationScoped
public class ChatWebSocket {

    private static final Logger LOG = Logger.getLogger(ChatWebSocket.class);

    @Inject
    MessageService messageService;

    @Inject
    SessionManager sessionManager;

    @Inject
    Util util;

    @Inject
    KafkaProducerService kafkaProducer;

    @Inject
    RedisService redisService;

    @Inject
    ManagedExecutor managedExecutor;

    @ConfigProperty(name = "server.id")
    String serverId;

    public static Map<Long, Session> onlineUsers = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        sessionManager.addSession(userId, session);
        redisService.setUserOnline(userId);
        LOG.info("User connected: " + userId);

        LOG.info("User connected: " + userId + " on port " + session.getRequestURI().getPort());

        managedExecutor.runAsync(() -> {
            List<ChatMessage> undeliveredMessages = messageService.getUndeliveredMessages(userId);
            undeliveredMessages.forEach(msg -> {
                SocketResponse response = new SocketResponse();
                response.messageId = msg.messageId;
                response.type = "CHAT";
                response.from = msg.from;
                response.to = msg.to;
                response.content = msg.content;
                response.timestamp = msg.timestamp;
                util.sendMessage(session, response);
            });

            util.sendMessage(session, "You have " + undeliveredMessages.size() + " undelivered messages.");
        });
    }

    @OnClose
    public void onClose(@PathParam("userId") Long userId) {
        sessionManager.removeSession(userId);
        redisService.setUserOffline(userId);
        LOG.info("User disconnected: " + userId);
    }

    @OnMessage
    public void onMessage(String message,
            @PathParam("userId") Long senderId) {

        LOG.info("Received message from user " + senderId + ": " + message);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(message);

            String type = jsonNode.path("type").asText("CHAT");

            if ("ACK".equals(type)) {

                if (!jsonNode.hasNonNull("messageId")) {
                    LOG.warn("ACK received without messageId: " + message);
                    return;
                }

                String messageId = jsonNode.get("messageId").asText().trim();
                if (messageId.isEmpty()) {
                    LOG.warn("ACK received with empty messageId: " + message);
                    return;
                }
                managedExecutor.runAsync(() -> {
                    messageService.updateMessageStatus(messageId, MessageStatus.DELIVERED);
                }).exceptionally(ex -> {
                    LOG.error("Failed to process ACK for messageId: " + messageId, ex);
                    return null;
                });

                LOG.info("ACK received for messageId: " + messageId);

                return;
            } else if ("CHAT".equals(type)) {
                if (!jsonNode.hasNonNull("to") || !jsonNode.hasNonNull("message")) {
                    LOG.warn("CHAT missing to/message: " + message);
                    return;
                }
                String messageId = UUID.randomUUID().toString();
                Long receiverId = jsonNode.get("to").asLong();
                String text = jsonNode.get("message").asText();
                if (redisService.isRateLimited(senderId)) {
                    LOG.warn("CHAT message from user " + senderId + " is rate limited.");
                    return;
                }
                managedExecutor.runAsync(() -> {
                    messageService.saveMessage(messageId, senderId, receiverId, text, MessageStatus.SENT);
                    ObjectNode payload = objectMapper.createObjectNode();
                    payload.put("type", "CHAT");
                    payload.put("from", senderId);
                    payload.put("to", receiverId);
                    payload.put("content", text);
                    payload.put("timestamp", System.currentTimeMillis());
                    payload.put("messageId", messageId);

                    LOG.info("Sending message to Kafka with messageId: " + messageId);
                    kafkaProducer.sendMessage(payload.toString());
                }).exceptionally(ex -> {
                    LOG.error("Failed to persist/send chat message with messageId: " + messageId, ex);
                    return null;
                });
                return;
            } else if ("READ".equals(type)) {
                if (!jsonNode.hasNonNull("messageId")) {
                    LOG.warn("READ received without messageId: " + message);
                    return;
                }

                String messageId = jsonNode.get("messageId").asText().trim();
                if (messageId.isEmpty()) {
                    LOG.warn("READ received with empty messageId: " + message);
                    return;
                }

                managedExecutor.runAsync(() -> {
                    messageService.updateMessageStatus(messageId, MessageStatus.READ);
                    Message msg = messageService.find(messageId);
                    if (msg == null) {
                        LOG.warn("READ received for unknown messageId: " + messageId);
                        return;
                    }

                    Long originalSenderId = msg.getSenderId();
                    String targetServer = redisService.getUserServer(originalSenderId).join();
                    if (targetServer == null) {
                        LOG.warn("Original sender " + originalSenderId + " is offline for messageId: " + messageId);
                        return;
                    }

                    ObjectNode readAck = objectMapper.createObjectNode();
                    readAck.put("type", "READ_ACK");
                    readAck.put("messageId", messageId);
                    readAck.put("readerId", senderId);
                    readAck.put("to", originalSenderId);

                    if (serverId.equals(targetServer)) {
                        Session senderSession = sessionManager.getSession(originalSenderId);
                        if (senderSession != null && senderSession.isOpen()) {
                            util.sendMessage(senderSession, readAck);
                        } else {
                            LOG.warn("Original sender session missing on server " + serverId + " for messageId: " + messageId);
                        }
                    } else {
                        kafkaProducer.sendMessage(readAck.toString());
                        LOG.info("Forwarded READ_ACK for messageId " + messageId + " to server " + targetServer);
                    }
                }).exceptionally(ex -> {
                    LOG.error("Failed to process READ for messageId: " + messageId, ex);
                    return null;
                });

                return;
            }
            LOG.warn("Unknown message type '" + type + "' from user " + senderId + ": " + message);
        } catch (Exception e) {
            LOG.error("Error occurred while processing message from user " + senderId, e);
        }
    }
}
