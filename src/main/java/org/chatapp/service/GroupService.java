package org.chatapp.service;

import java.time.LocalDateTime;

import org.chatapp.entity.Group;
import org.chatapp.repo.GroupRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GroupService {
    @Inject
    private GroupRepository groupRepository;
    public void createGroup(String groupName, Long creatorId) {
        Group group = new Group();
        group.setName(groupName);
        group.setCreatedBy(creatorId);
        group.setCreatedAt(LocalDateTime.now());

        groupRepository.persist(group);
    }
}
