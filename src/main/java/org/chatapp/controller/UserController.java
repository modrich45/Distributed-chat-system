package org.chatapp.controller;

import org.chatapp.entity.User;
import org.chatapp.service.UserService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserController {
    @Inject
    UserService userService;

    @POST
    public User createUser(User user) {
        userService.persistUser(user);
        return user;
    }
}
