package org.chatapp.service;

import org.chatapp.entity.User;
import org.chatapp.repo.UserRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class UserService {

    @Inject
    UserRepository userRepository;

    @Transactional
    public User persistUser(User user) {
        userRepository.persist(user);
        return user;
    }
}
