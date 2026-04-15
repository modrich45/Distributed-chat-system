package org.chatapp.user.service;

import org.chatapp.user.entity.User;
import org.chatapp.user.repo.UserRepository;

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
