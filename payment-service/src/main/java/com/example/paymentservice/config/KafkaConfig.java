package com.example.paymentservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String BILLING_CREATED_TOPIC = "billing-created";
    public static final String PAYMENT_PROCESSED_TOPIC = "payment-processed";
    public static final String INVOICE_GENERATED_TOPIC = "invoice-generated";
    public static final String PRESCRIPTION_RECEIVED_TOPIC = "prescription-received";

    @Bean
    public NewTopic billingCreatedTopic() {
        return TopicBuilder.name(BILLING_CREATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentProcessedTopic() {
        return TopicBuilder.name(PAYMENT_PROCESSED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic invoiceGeneratedTopic() {
        return TopicBuilder.name(INVOICE_GENERATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}

