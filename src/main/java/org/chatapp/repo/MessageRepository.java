package org.chatapp.repo;

import java.time.LocalDateTime;
import java.util.List;

import org.chatapp.dto.ChatHistoryResponse;
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

    public List<ChatHistoryResponse> getMessageHistory(Long userId1, Long userId2, LocalDateTime before, int limit) {
        List<Message> messages = find(
                "((senderId = ?1 and receiverId = ?2) or (senderId = ?2 and receiverId = ?1)) and timestamp < ?3 order by timestamp desc",
                userId1, userId2, before).page(0, limit)
                .list();

        ChatHistoryResponse response = new ChatHistoryResponse();
        response.messages = messages;
        response.hasMore = messages.size() == limit;
        if (!messages.isEmpty()) {
            response.nextCursor = messages.get(messages.size() - 1).getTimestamp();
        }
        return List.of(response);
    }
}
