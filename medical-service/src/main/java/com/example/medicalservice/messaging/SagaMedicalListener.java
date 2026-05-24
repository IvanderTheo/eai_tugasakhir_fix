package com.example.medicalservice.messaging;

import com.example.medicalservice.config.KafkaConfig;
import com.example.medicalservice.entity.SagaInstance;
import com.example.medicalservice.service.ResepService;
import com.example.medicalservice.service.SagaInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class SagaMedicalListener {

    @Autowired
    private ResepService resepService;
    
    @Autowired
    private SagaInstanceService sagaInstanceService;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Listen to prescription-received events for stock confirmation
     * Implements: Idempotency + State Tracking
     */
    @KafkaListener(topics = KafkaConfig.PRESCRIPTION_RECEIVED_TOPIC, groupId = "medical-saga-group")
    @Transactional
    public void listenPrescriptionReceived(
            Map<String, Object> event, 
            Acknowledgment ack,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String messageKey) {
        
        String messageId = messageKey != null ? messageKey : UUID.randomUUID().toString();
        log.info("Received prescription-received event in medical-service with messageId: {}", messageId);
        
        SagaInstance sagaInstance = null;
        try {
            // ✅ IDEMPOTENCY: Check if message already processed
            if (sagaInstanceService.isMessageAlreadyProcessed(messageId)) {
                log.warn("⚠️ IDEMPOTENCY: Message {} already processed, skipping", messageId);
                ack.acknowledge();
                return;
            }

            Long prescriptionId = Long.valueOf(event.get("prescriptionId").toString());
            String status = (String) event.get("status");
            String sagaId = (String) event.getOrDefault("sagaId", "unknown");

            // ✅ TRACK SAGA INSTANCE
            sagaInstance = sagaInstanceService.getOrCreateSagaInstance(
                messageId,
                "prescription-received",
                event
            );

            if ("STOCK_RESERVED".equals(status)) {
                log.info("✅ Updating prescription ID: {} status to RESERVED", prescriptionId);
                resepService.updateResepStatus(prescriptionId, "RESERVED");
                sagaInstanceService.updateSagaStatus(sagaId, "IN_PROGRESS", null);
            } else if ("STOCK_FAILED".equals(status)) {
                log.warn("❌ Updating prescription ID: {} status to FAILED_OUT_OF_STOCK", prescriptionId);
                resepService.updateResepStatus(prescriptionId, "FAILED_OUT_OF_STOCK");
                sagaInstanceService.updateSagaStatus(sagaId, "FAILED", "Stock not reserved");
            }
        } catch (Exception e) {
            log.error("❌ Error processing prescription-received event with messageId: {}", messageId, e);
            if (sagaInstance != null) {
                sagaInstanceService.markSagaFailed(sagaInstance.getSagaId(), 
                    "Exception: " + e.getMessage());
            }
            // Send to DLQ on error
            try {
                event.put("messageId", messageId);
                event.put("dlqReason", "Error in listenPrescriptionReceived: " + e.getMessage());
                kafkaTemplate.send(KafkaConfig.PRESCRIPTION_RECEIVED_DLQ, messageId, event);
                log.warn("Sent event to DLQ: {}", KafkaConfig.PRESCRIPTION_RECEIVED_DLQ);
            } catch (Exception dlqError) {
                log.error("Failed to send to DLQ", dlqError);
            }
        } finally {
            ack.acknowledge();
        }
    }

    /**
     * Listen to payment-processed events for prescription completion/cancellation
     * Implements: Idempotency + State Tracking
     */
    @KafkaListener(topics = "payment-processed", groupId = "medical-saga-group")
    @Transactional
    public void listenPaymentProcessed(
            Map<String, Object> event, 
            Acknowledgment ack,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String messageKey) {
        
        String messageId = messageKey != null ? messageKey : UUID.randomUUID().toString();
        log.info("Received payment-processed event in medical-service with messageId: {}", messageId);
        
        SagaInstance sagaInstance = null;
        try {
            // ✅ IDEMPOTENCY: Check if message already processed
            if (sagaInstanceService.isMessageAlreadyProcessed(messageId)) {
                log.warn("⚠️ IDEMPOTENCY: Message {} already processed, skipping", messageId);
                ack.acknowledge();
                return;
            }

            Long prescriptionId = Long.valueOf(event.get("resepId").toString());
            String status = (String) event.get("status");
            String sagaId = (String) event.getOrDefault("sagaId", "unknown");

            // ✅ TRACK SAGA INSTANCE
            sagaInstance = sagaInstanceService.getOrCreateSagaInstance(
                messageId,
                "payment-processed",
                event
            );

            if ("SUCCESS".equals(status)) {
                log.info("✅ Updating prescription ID: {} status to COMPLETED (Payment Success)", prescriptionId);
                resepService.updateResepStatus(prescriptionId, "COMPLETED");
                sagaInstanceService.updateSagaStatus(sagaId, "COMPLETED", null);
            } else if ("FAILED".equals(status)) {
                log.warn("❌ Updating prescription ID: {} status to CANCELLED (Payment Failed/Cancelled)", prescriptionId);
                resepService.updateResepStatus(prescriptionId, "CANCELLED");
                sagaInstanceService.markCompensationExecuted(sagaId);
            }
        } catch (Exception e) {
            log.error("❌ Error processing payment-processed event with messageId: {}", messageId, e);
            if (sagaInstance != null) {
                sagaInstanceService.markSagaFailed(sagaInstance.getSagaId(), 
                    "Exception: " + e.getMessage());
            }
            // Send to DLQ on error
            try {
                event.put("messageId", messageId);
                event.put("dlqReason", "Error in listenPaymentProcessed: " + e.getMessage());
                kafkaTemplate.send(KafkaConfig.PAYMENT_PROCESSED_DLQ, messageId, event);
                log.warn("Sent event to DLQ: {}", KafkaConfig.PAYMENT_PROCESSED_DLQ);
            } catch (Exception dlqError) {
                log.error("Failed to send to DLQ", dlqError);
            }
        } finally {
            ack.acknowledge();
        }
    }
}
