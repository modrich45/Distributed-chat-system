package org.chatapp.util;

import org.chatapp.service.RetryService;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.Session;

@ApplicationScoped
public class Util {

    @Inject
    RetryService retryService;

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final Logger LOG = Logger.getLogger(Util.class);

    public void sendMessage(
            Session session,
            Object payload) {

        if (session != null &&
                session.isOpen()) {

            try {

                String json = mapper
                        .writeValueAsString(
                                payload);

                retryService.retry(

                        () -> {

                            session
                                    .getAsyncRemote()
                                    .sendText(json);

                        },

                        3

                );

            }

            catch (Exception e) {

                LOG.error(
                        "Send failed",
                        e);

            }

        }

    }

}
