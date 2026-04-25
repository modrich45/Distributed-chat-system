package org.chatapp.service;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class KafkaProducerService {
    @Inject
    @Channel("chat-out")
    Emitter<String> emitter;

    public void sendMessage(String message) {
        emitter.send(message);
        System.out.println("Message sent from KafkaProducerService: " + message);
    }
}
