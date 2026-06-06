package org.chatapp.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.chatapp.entity.Message;

public class ChatHistoryResponse {

    public List<Message> messages;

    public LocalDateTime nextCursor;

    public boolean hasMore;
}
