package com.natsmonitor.service;

import com.natsmonitor.config.NatsConnectionOptionsFactory;
import com.natsmonitor.config.NatsMonitoringConfig;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Objects;

@Service
public class NatsSystemEventListener {
    private static final Logger log = LoggerFactory.getLogger(NatsSystemEventListener.class);

    private final NatsMonitoringConfig config;
    private final NatsConnectionOptionsFactory optionsFactory;
    private final NatsEventService eventService;
    private Connection connection;
    private Dispatcher dispatcher;

    public NatsSystemEventListener(
            NatsMonitoringConfig config,
            NatsConnectionOptionsFactory optionsFactory,
            NatsEventService eventService) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.optionsFactory = Objects.requireNonNull(optionsFactory, "optionsFactory must not be null");
        this.eventService = Objects.requireNonNull(eventService, "eventService must not be null");
    }

    @PostConstruct
    public void start() {
        if (!config.isSystemEventsEnabled()) {
            log.info("NATS system event listener is disabled");
            return;
        }
        try {
            connection = Nats.connect(optionsFactory.create(config));
            dispatcher = connection.createDispatcher(message -> {
                try {
                    eventService.record(message.getSubject(), message.getData());
                } catch (Exception e) {
                    log.warn("Failed to record NATS event {}: {}", message.getSubject(), e.getMessage());
                }
            });
            dispatcher.subscribe("$SYS.>");
            dispatcher.subscribe("$JS.EVENT.ADVISORY.>");
            log.info("NATS system event listener subscribed to $SYS.> and $JS.EVENT.ADVISORY.>");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Unable to start NATS system event listener: {}", e.getMessage());
        } catch (IOException e) {
            log.warn("Unable to start NATS system event listener: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Unable to start NATS system event listener: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void stop() {
        try {
            if (dispatcher != null && connection != null) {
                connection.closeDispatcher(dispatcher);
            }
            if (connection != null) {
                connection.close();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
