package org.chatapp.repo;


import java.util.List;

import org.chatapp.entity.Message;
import org.chatapp.enums.MessageStatus;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MessageRepository implements PanacheRepository<Message> {
    public List<Message> undeliveredMessages(Long receiverId) {
        return find("receiverId = ?1 and status = ?2", receiverId, MessageStatus.SENT).list();
    }

    public Message findByMessageId(String messageId) {
        return find("messageId", messageId).firstResult();
    }

    public long markDeliveredByMessageId(String messageId) {
        return update("status = ?1 where messageId = ?2", MessageStatus.DELIVERED, messageId);
    }
}
