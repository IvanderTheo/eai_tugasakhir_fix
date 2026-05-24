package com.example.pharmacyservice.messaging;

import com.example.pharmacyservice.config.KafkaConfig;
import com.example.pharmacyservice.service.SagaInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@Slf4j
public class SagaDeadLetterQueueHandler {
    
    @Autowired
    private SagaInstanceService sagaInstanceService;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = KafkaConfig.PRESCRIPTION_RECEIVED_DLQ, groupId = "pharmacy-dlq-handler")
    public void handlePrescriptionReceivedDlq(Map<String, Object> event, Acknowledgment ack) {
        log.error("DLQ Handler - Processing failed message from prescription-received DLQ: {}", event);
        handleDeadLetterMessage(event, "prescription-received");
        ack.acknowledge();
    }

    @KafkaListener(topics = KafkaConfig.STOCK_UPDATED_DLQ, groupId = "pharmacy-dlq-handler")
    public void handleStockUpdatedDlq(Map<String, Object> event, Acknowledgment ack) {
        log.error("DLQ Handler - Processing failed message from stock-updated DLQ: {}", event);
        handleDeadLetterMessage(event, "stock-updated");
        ack.acknowledge();
    }

    @KafkaListener(topics = KafkaConfig.PAYMENT_PROCESSED_DLQ, groupId = "pharmacy-dlq-handler")
    public void handlePaymentProcessedDlq(Map<String, Object> event, Acknowledgment ack) {
        log.error("DLQ Handler - Processing failed message from payment-processed DLQ: {}", event);
        handleDeadLetterMessage(event, "payment-processed");
        ack.acknowledge();
    }

    private void handleDeadLetterMessage(Map<String, Object> event, String originTopic) {
        try {
            String messageId = (String) event.getOrDefault("messageId", "unknown");
            String prescriptionId = event.getOrDefault("prescriptionId", event.getOrDefault("resepId", "unknown")).toString();
            
            log.warn("DLQ: Dead letter message received from topic: {} for prescriptionId: {}", originTopic, prescriptionId);
            
            // Update saga instance with failed status
            if (event.containsKey("sagaId")) {
                String sagaId = (String) event.get("sagaId");
                String errorMsg = (String) event.getOrDefault("dlqReason", "Message processing failed and moved to DLQ");
                sagaInstanceService.markSagaFailed(sagaId, errorMsg);
            }
            
            // Log to monitoring/alerting system
            log.error("ALERT: DLQ Message - Topic: {}, PrescriptionId: {}, MessageId: {}, Timestamp: {}", 
                originTopic, prescriptionId, messageId, LocalDateTime.now());
            
            // TODO: Send alert to monitoring system / admin notification
            // TODO: Implement retry logic with exponential backoff
            
        } catch (Exception e) {
            log.error("Error handling dead letter message", e);
        }
    }
}
