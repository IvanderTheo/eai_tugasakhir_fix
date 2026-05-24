package com.example.pharmacyservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String PRESCRIPTION_RECEIVED_TOPIC = "prescription-received";
    public static final String STOCK_UPDATED_TOPIC = "stock-updated";
    public static final String STOCK_LOW_ALERT_TOPIC = "stock-low-alert";

    @Bean
    public NewTopic prescriptionReceivedTopic() {
        return TopicBuilder.name(PRESCRIPTION_RECEIVED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic stockUpdatedTopic() {
        return TopicBuilder.name(STOCK_UPDATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic stockLowAlertTopic() {
        return TopicBuilder.name(STOCK_LOW_ALERT_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}

