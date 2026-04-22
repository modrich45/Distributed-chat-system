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

import org.chatapp.dto.ChatMessage;
import org.chatapp.dto.SocketResponse;
import org.chatapp.entity.Message;
import org.chatapp.service.MessageService;
import org.chatapp.service.RedisService;
import org.chatapp.util.Util;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ServerEndpoint("/chat/{userId}")
public class ChatWebSocket {

    private static final Logger LOG = Logger.getLogger(ChatWebSocket.class);

    @Inject
    MessageService messageService;

    @Inject
    Util util;

    @Inject 
    RedisService redisService;

    private static Map<Long, Session> onlineUsers = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        onlineUsers.put(userId, session);
        redisService.setUserOnline(userId);
        LOG.info("User connected: " + userId);

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
        onlineUsers.remove(userId);
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

            CompletableFuture.runAsync(() -> messageService.saveMessage(senderId, receiverId, text, false));

            Session receiverSession = onlineUsers.get(receiverId);

            if (receiverSession != null && receiverSession.isOpen() && redisService.isUserOnline(receiverId).join()) {
                SocketResponse response = new SocketResponse();
                response.type = "CHAT";
                response.from = senderId;
                response.to = receiverId;
                response.content = text;
                Util.sendMessage(receiverSession, response);
            } else {
                LOG.warn("User " + receiverId + " is not online.");
            }

        } catch (Exception e) {
            LOG.error("Error occurred while processing message from user " + senderId, e);
        }
    }
}