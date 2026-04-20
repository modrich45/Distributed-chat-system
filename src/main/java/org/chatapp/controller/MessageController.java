package org.chatapp.controller;

import java.util.List;

import org.chatapp.entity.Message;
import org.chatapp.service.MessageService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;

@ApplicationScoped
public class MessageController {

    @Inject 
    MessageService messageService;
    
    @GET
    public List<Message> getAllMessages() {
        return messageService.getAllMessages();
    }

}
