package org.chatapp.webSocket;

import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.chatapp.dto.ChatMessage;
import org.chatapp.entity.Message;
import org.chatapp.service.MessageService;
import org.chatapp.util.Util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ServerEndpoint("/chat/{userId}")
public class ChatWebSocket {

    @Inject
    MessageService messageService;

    @Inject
    Util util;

    private static Map<Long, Session> onlineUsers = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        onlineUsers.put(userId, session);
        System.out.println("User connected: " + userId);

        CompletableFuture.runAsync(() -> {
            List<ChatMessage> undeliveredMessages = messageService.getUndeliveredMessages(userId);
            undeliveredMessages.forEach(msg -> {
                util.sendMessage(session, "From " + msg.from + ": " + msg.content); 
            });
            
            util.sendMessage(session, "You have " + undeliveredMessages.size() + " undelivered messages.");
        });
    }

    @OnClose
    public void onClose(@PathParam("userId") Long userId) {
        onlineUsers.remove(userId);
        System.out.println("User disconnected: " + userId);
    }

    @OnMessage
    public void onMessage(String message,
            @PathParam("userId") Long senderId) {

        System.out.println("Received message from user " + senderId + ": " + message);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(message);

            Long receiverId = jsonNode.get("to").asLong();
            String text = jsonNode.get("message").asText();

            CompletableFuture.runAsync(() -> messageService.saveMessage(senderId, receiverId, text, false));

            Session receiverSession = onlineUsers.get(receiverId);

            if (receiverSession != null && receiverSession.isOpen()) {
                util.sendMessage(receiverSession, "From " + senderId + ": " + text);
            } else {
                System.out.println("User " + receiverId + " is not online.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}