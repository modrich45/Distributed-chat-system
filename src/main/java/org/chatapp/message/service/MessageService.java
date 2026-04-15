package org.chatapp.message.service;

import java.util.List;

import org.chatapp.message.entity.Message;
import org.chatapp.message.repo.MessageRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MessageService {

    @Inject
    MessageRepository messageRepository;

    public List<Message> getAllMessages() {
        return messageRepository.listAll();
    }
}
