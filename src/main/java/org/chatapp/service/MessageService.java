package org.chatapp.service;

import java.time.LocalDateTime;
import java.util.List;

import org.chatapp.dto.ChatMessage;
import org.chatapp.entity.Message;
import org.chatapp.repo.MessageRepository;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class MessageService {
    private static final Logger LOG = Logger.getLogger(MessageService.class);

    @Inject
    MessageRepository messageRepository;

    @Transactional
    public Message saveMessage(String messageId, Long senderId, Long receiverId, String content, boolean delivered) {
        Message msg = new Message();
        msg.setMessageId(messageId);
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setContent(content);
        msg.setTimestamp(LocalDateTime.now());
        msg.setDelivered(delivered);
        messageRepository.persist(msg);
        System.out.println("Message saved to database: " + msg.getContent());
        return msg;
    }

    @Transactional
    public List<ChatMessage> getUndeliveredMessages(Long receiverId) {
        List<Message> undelivered = messageRepository.undeliveredMessages(receiverId);
        return undelivered.stream().map(msg -> {
            ChatMessage chatMsg = new ChatMessage();
            chatMsg.messageId = msg.getMessageId();
            chatMsg.from = msg.getSenderId();
            chatMsg.to = msg.getReceiverId();
            chatMsg.content = msg.getContent();
            chatMsg.timestamp = msg.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            return chatMsg;
        }).toList();
    }

    @Transactional
    public void markAsDelivered(String messageId) {
        String normalizedMessageId = messageId == null ? null : messageId.trim();
        if (normalizedMessageId == null || normalizedMessageId.isEmpty()) {
            LOG.warn("Cannot mark delivered. messageId is null/empty.");
            return;
        }

        long updatedRows = messageRepository.markDeliveredByMessageId(normalizedMessageId);
        if (updatedRows > 0) {
            LOG.info("Marked message delivered for messageId: " + messageId);
        } else {
            LOG.warn("No message found to mark delivered for messageId: " + messageId);
        }
    }

    public List<Message> getAllMessages() {
        return messageRepository.listAll();
    }
}
