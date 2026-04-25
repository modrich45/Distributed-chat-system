package org.chatapp.webSocket;

import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.chatapp.dto.ChatMessage;
import org.chatapp.dto.SocketResponse;
import org.chatapp.service.KafkaProducerService;
import org.chatapp.service.MessageService;
import org.chatapp.service.RedisService;
import org.chatapp.util.SessionManager;
import org.chatapp.util.Util;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@ServerEndpoint("/chat/{userId}")
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

    public static Map<Long, Session> onlineUsers = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        sessionManager.addSession(userId, session);
        redisService.setUserOnline(userId);
        LOG.info("User connected: " + userId);

        LOG.info("User connected: " + userId + " on port " + session.getRequestURI().getPort());

        CompletableFuture.runAsync(() -> {
            List<ChatMessage> undeliveredMessages = messageService.getUndeliveredMessages(userId);
            undeliveredMessages.forEach(msg -> {
                SocketResponse response = new SocketResponse();
                response.type = "CHAT";
                response.from = msg.from;
                response.to = msg.to;
                response.content = msg.content;
                response.timestamp = msg.timestamp;
                Util.sendMessage(session, response);
            });

            Util.sendMessage(session, "You have " + undeliveredMessages.size() + " undelivered messages.");
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

            Long receiverId = jsonNode.get("to").asLong();
            String text = jsonNode.get("message").asText();

            CompletableFuture.runAsync(() -> {
                messageService.saveMessage(senderId, receiverId, text, false);
            });
            LOG.info("Sending message to Kafka: " + message);

            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("from", senderId);
            payload.put("to", receiverId);
            payload.put("content", text);
            payload.put("timestamp", System.currentTimeMillis());

            kafkaProducer.sendMessage(payload.toString());

        } catch (Exception e) {
            LOG.error("Error occurred while processing message from user " + senderId, e);
        }
    }
}