package org.chatapp.message.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Message {
    @Id
    @GeneratedValue
    public Long id;

    public Long senderId;
    public Long receiverId;

    public String content;

    public LocalDateTime timestamp;
}
