package com.example.adminservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    // Admin Service Topics
    public static final String PATIENT_CREATED_TOPIC = "patient-created";
    public static final String PATIENT_UPDATED_TOPIC = "patient-updated";
    public static final String USER_REGISTERED_TOPIC = "user-registered";
    public static final String SCHEDULE_CREATED_TOPIC = "schedule-created";

    // Medical Service Topics
    public static final String MEDICAL_RECORD_CREATED_TOPIC = "medical-record-created";
    public static final String PRESCRIPTION_CREATED_TOPIC = "prescription-created";
    public static final String DIAGNOSIS_RECORDED_TOPIC = "diagnosis-recorded";

    // Pharmacy Service Topics
    public static final String PRESCRIPTION_RECEIVED_TOPIC = "prescription-received";
    public static final String STOCK_UPDATED_TOPIC = "stock-updated";
    public static final String STOCK_LOW_ALERT_TOPIC = "stock-low-alert";

    // Payment Service Topics
    public static final String BILLING_CREATED_TOPIC = "billing-created";
    public static final String PAYMENT_PROCESSED_TOPIC = "payment-processed";
    public static final String INVOICE_GENERATED_TOPIC = "invoice-generated";

    // Topic Beans
    @Bean
    public NewTopic patientCreatedTopic() {
        return TopicBuilder.name(PATIENT_CREATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic patientUpdatedTopic() {
        return TopicBuilder.name(PATIENT_UPDATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userRegisteredTopic() {
        return TopicBuilder.name(USER_REGISTERED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic scheduleCreatedTopic() {
        return TopicBuilder.name(SCHEDULE_CREATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic medicalRecordCreatedTopic() {
        return TopicBuilder.name(MEDICAL_RECORD_CREATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic prescriptionCreatedTopic() {
        return TopicBuilder.name(PRESCRIPTION_CREATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic diagnosisRecordedTopic() {
        return TopicBuilder.name(DIAGNOSIS_RECORDED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

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

