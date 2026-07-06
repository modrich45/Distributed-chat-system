package org.chatapp.service;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RetryService {
    @Inject
    ManagedExecutor executor;

    private static Logger LOG = Logger.getLogger(RetryService.class);
    public void retry(Runnable task, int maxRetries) {
        executor.execute(() -> {
            int attempt = 0;
            while (attempt < maxRetries) {
                try {
                    task.run();
                    return;
                } catch (Exception e) {
                attempt++; 
                    if (attempt >= maxRetries) {
                        // Log failure after max retries
                        LOG.error("Task failed after " + maxRetries + " attempts: " + e.getMessage());
                    } else {
                        // Log retry attempt
                        long delayMillis = (long) Math.pow(2, attempt) * 1000; // Exponential backoff
                        LOG.info("Task failed, retrying attempt " + attempt + " after " + delayMillis + "ms");
                        try {
                            Thread.sleep(delayMillis);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return; // Exit if interrupted
                        }
                    }
                }
            }
        });
    }
}
