package org.chatapp.service;

import java.time.LocalDateTime;
import java.util.List;

import org.chatapp.dto.ChatMessage;
import org.chatapp.entity.Message;
import org.chatapp.enums.MessageStatus;
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
    public Message saveMessage(String messageId, Long senderId, Long receiverId, String content, MessageStatus status) {
        Message msg = new Message();
        msg.setStatus(status);
        msg.setMessageId(messageId);
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setContent(content);
        msg.setStatus(status);
        msg.setTimestamp(LocalDateTime.now());
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
    public void updateMessageStatus(
            String messageId,
            MessageStatus status) {

        Message msg = messageRepository.findByMessageId(messageId);

        if (msg != null) {
            msg.setStatus(status);
        }
    }

    @Transactional
    public Message find(String messageId) {
        return messageRepository.findByMessageId(messageId);
    }

    public List<Message> getAllMessages() {
        return messageRepository.listAll();
    }
}
