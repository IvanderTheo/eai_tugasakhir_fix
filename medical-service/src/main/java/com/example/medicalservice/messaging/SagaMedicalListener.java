package com.example.medicalservice.messaging;

import com.example.medicalservice.config.KafkaConfig;
import com.example.medicalservice.service.ResepService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class SagaMedicalListener {

    @Autowired
    private ResepService resepService;

    @KafkaListener(topics = KafkaConfig.PRESCRIPTION_RECEIVED_TOPIC, groupId = "medical-saga-group")
    public void listenPrescriptionReceived(Map<String, Object> event, Acknowledgment ack) {
        log.info("Received prescription-received event in medical-service: {}", event);
        try {
            Long prescriptionId = Long.valueOf(event.get("prescriptionId").toString());
            String status = (String) event.get("status");

            if ("STOCK_RESERVED".equals(status)) {
                log.info("Updating prescription ID: {} status to RESERVED", prescriptionId);
                resepService.updateResepStatus(prescriptionId, "RESERVED");
            } else if ("STOCK_FAILED".equals(status)) {
                log.warn("Updating prescription ID: {} status to FAILED_OUT_OF_STOCK", prescriptionId);
                resepService.updateResepStatus(prescriptionId, "FAILED_OUT_OF_STOCK");
            }
        } catch (Exception e) {
            log.error("Error processing prescription-received event", e);
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = "payment-processed", groupId = "medical-saga-group")
    public void listenPaymentProcessed(Map<String, Object> event, Acknowledgment ack) {
        log.info("Received payment-processed event in medical-service: {}", event);
        try {
            Long prescriptionId = Long.valueOf(event.get("resepId").toString());
            String status = (String) event.get("status");

            if ("SUCCESS".equals(status)) {
                log.info("Updating prescription ID: {} status to COMPLETED (Payment Success)", prescriptionId);
                resepService.updateResepStatus(prescriptionId, "COMPLETED");
            } else if ("FAILED".equals(status)) {
                log.warn("Updating prescription ID: {} status to CANCELLED (Payment Failed/Cancelled)", prescriptionId);
                resepService.updateResepStatus(prescriptionId, "CANCELLED");
            }
        } catch (Exception e) {
            log.error("Error processing payment-processed event", e);
        } finally {
            ack.acknowledge();
        }
    }
}
