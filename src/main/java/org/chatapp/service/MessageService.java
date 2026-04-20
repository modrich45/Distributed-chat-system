package org.chatapp.service;

import java.time.LocalDateTime;
import java.util.List;

import org.chatapp.dto.ChatMessage;
import org.chatapp.entity.Message;
import org.chatapp.repo.MessageRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class MessageService {

    @Inject
    MessageRepository messageRepository;

    @Transactional
    public void saveMessage(Long senderId, Long receiverId, String content, boolean delivered) {
        Message msg = new Message();
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setContent(content);
        msg.setTimestamp(LocalDateTime.now());
        msg.setDelivered(delivered);
        messageRepository.persist(msg);
        System.out.println("Message saved to database: " + msg.getContent());
    }

    @Transactional
    public List<ChatMessage> getUndeliveredMessages(Long receiverId) {
        List<Message> undelivered = messageRepository.undeliveredMessages(receiverId);
        undelivered.forEach(msg -> {
            msg.setDelivered(true);
            messageRepository.persist(msg);
        });
        return undelivered.stream().map(msg -> {
            ChatMessage chatMsg = new ChatMessage();
            chatMsg.messageId = msg.getId();
            chatMsg.from = msg.getSenderId();
            chatMsg.to = msg.getReceiverId();
            chatMsg.content = msg.getContent();
            chatMsg.timestamp = msg.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            return chatMsg;
        }).toList();
    }


    @Transactional
    public void markMessageAsDelivered(Long receiverId, Long senderId, String content) {
        
    }

    public List<Message> getAllMessages() {
        return messageRepository.listAll();
    }
}
