package org.chatapp.message.service;

import java.time.LocalDateTime;
import java.util.List;

import org.chatapp.message.entity.Message;
import org.chatapp.message.repo.MessageRepository;

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
        msg.senderId = senderId;
        msg.receiverId = receiverId;
        msg.content = content;
        msg.timestamp = LocalDateTime.now();
        msg.delivered = delivered;
        messageRepository.persist(msg);
        System.out.println("Message saved to database: " + msg.content);
    }

    @Transactional
    public List<Message> getUndeliveredMessages(Long receiverId) {
        List<Message> undelivered = messageRepository.undeliveredMessages(receiverId);
        undelivered.forEach(msg -> {
            msg.delivered = true;
            messageRepository.persist(msg);
        });
        return undelivered;
    }


    @Transactional
    public void markMessageAsDelivered(Long receiverId, Long senderId, String content) {
        
    }

    public List<Message> getAllMessages() {
        return messageRepository.listAll();
    }
}
