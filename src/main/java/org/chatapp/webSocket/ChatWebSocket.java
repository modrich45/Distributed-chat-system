package org.chatapp.webSocket;

import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.chatapp.message.entity.Message;
import org.chatapp.message.service.MessageService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ServerEndpoint("/chat/{userId}")
public class ChatWebSocket {

    @Inject
    MessageService messageService;

    private static Map<Long, Session> onlineUsers = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        onlineUsers.put(userId, session);
        System.out.println("User connected: " + userId);

        CompletableFuture.runAsync(() -> {
            List<Message> undeliveredMessages = messageService.getUndeliveredMessages(userId);
            onlineUsers.get(userId).getAsyncRemote()
                    .sendText("You have " + undeliveredMessages.size() + " undelivered messages.");
            undeliveredMessages.forEach(msg -> {
                onlineUsers.get(userId).getAsyncRemote().sendText("From " + msg.senderId + ": " + msg.content);
            });
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
                receiverSession.getAsyncRemote().sendText("From" + senderId + ": " + text);
            } else {
                System.out.println("User " + receiverId + " is not online.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}