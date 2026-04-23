package com.natsmonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NatsMonitoringApplication {

    public static void main(String[] args) {
        SpringApplication.run(NatsMonitoringApplication.class, args);
    }
}
