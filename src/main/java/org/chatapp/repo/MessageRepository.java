package org.chatapp.repo;


import java.util.List;

import org.chatapp.entity.Message;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MessageRepository implements PanacheRepository<Message> {
    public List<Message> undeliveredMessages(Long receiverId) {
        return find("receiverId = ?1 and delivered = false", receiverId).list();
    }

    public Message findByMessageId(String messageId) {
        return find("messageId", messageId).firstResult();
    }

    public long markDeliveredByMessageId(String messageId) {
        return update("delivered = true where messageId = ?1", messageId);
    }
}
