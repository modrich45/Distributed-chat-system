package org.chatapp.message.repo;


import org.chatapp.message.entity.Message;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MessageRepository implements PanacheRepository<Message> {

}
