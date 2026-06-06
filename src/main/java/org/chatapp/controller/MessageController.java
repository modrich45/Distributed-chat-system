package org.chatapp.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.chatapp.entity.Message;
import org.chatapp.service.MessageService;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.server.PathParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;


@Path("/messages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class MessageController {

    @Inject
    MessageService messageService;

    @GET
    public List<Message> getAllMessages() {
        return messageService.getAllMessages();
    }

    @GET
    @Path("/{user1}/{user2}")
    public List<Message> getMessages(
            @PathParam("user1") Long user1,
            @PathParam("user2") Long user2,

            @QueryParam("before") String before,

            @QueryParam("limit") @DefaultValue("20") int limit) {

        LocalDateTime beforeTime = LocalDateTime.parse(before);

        if(beforeTime == null) {
            beforeTime = LocalDateTime.now();
        }

        return messageService.getMessageHistory(
                user1,
                user2,
                beforeTime,
                limit);
    }

}
