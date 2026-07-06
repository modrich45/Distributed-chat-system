package org.chatapp.controller;

import org.chatapp.dto.CreateGroupRequest;
import org.chatapp.service.GroupService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/groups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GroupController {

    @Inject
    private GroupService groupService;

    @POST
    @Path("/create")
    public String createGroup(CreateGroupRequest request) {
        groupService.createGroup(request.groupName, request.creatorId);
        return "Group created successfully!";
    }

}
