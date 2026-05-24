package com.example.pharmacyservice.messaging;

import com.example.pharmacyservice.config.KafkaConfig;
import com.example.pharmacyservice.entity.Obat;
import com.example.pharmacyservice.entity.SagaInstance;
import com.example.pharmacyservice.repository.ObatRepository;
import com.example.pharmacyservice.service.SagaInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class SagaPharmacyListener {

    @Autowired
    private ObatRepository obatRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Autowired
    private SagaInstanceService sagaInstanceService;

    /**
     * Listen to prescription-created events and reserve stock
     * Implements: Saga Initiation + Idempotency + State Tracking
     */
    @KafkaListener(topics = "prescription-created", groupId = "pharmacy-saga-group")
    @Transactional
    public void listenPrescriptionCreated(
            Map<String, Object> event, 
            Acknowledgment ack,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String messageKey) {
        
        String messageId = messageKey != null ? messageKey : UUID.randomUUID().toString();
        log.info("Received prescription-created event in pharmacy-service with messageId: {}", messageId);
        
        SagaInstance sagaInstance = null;
        try {
            Long prescriptionId = Long.valueOf(event.get("id").toString());
            Long patientId = Long.valueOf(event.get("pasienId").toString());
            String namaObat = (String) event.get("namaObat");
            Integer jumlah = Integer.valueOf(event.get("jumlah").toString());

            // ✅ IDEMPOTENCY: Check if message already processed
            if (sagaInstanceService.isMessageAlreadyProcessed(messageId)) {
                log.warn("⚠️ IDEMPOTENCY: Message {} already processed, skipping", messageId);
                ack.acknowledge();
                return;
            }

            // ✅ CREATE SAGA INSTANCE for tracking
            sagaInstance = sagaInstanceService.getOrCreateSagaInstance(
                messageId, 
                "prescription-created", 
                event
            );
            String sagaId = sagaInstance.getSagaId();
            log.info("Created saga instance: {} for prescriptionId: {}", sagaId, prescriptionId);

            Optional<Obat> optionalObat = obatRepository.findByNamaObat(namaObat);
            
            Map<String, Object> responseEvent = new HashMap<>();
            responseEvent.put("prescriptionId", prescriptionId);
            responseEvent.put("pasienId", patientId);
            responseEvent.put("namaObat", namaObat);
            responseEvent.put("jumlah", jumlah);
            responseEvent.put("sagaId", sagaId);
            responseEvent.put("messageId", messageId);

            if (optionalObat.isPresent()) {
                Obat obat = optionalObat.get();
                if (obat.getStok() >= jumlah) {
                    // Reserve stock
                    obat.setStok(obat.getStok() - jumlah);
                    obatRepository.save(obat);
                    log.info("✅ Stock reserved for prescriptionId: {}, medicine: {}, remaining stock: {}", 
                            prescriptionId, namaObat, obat.getStok());

                    responseEvent.put("status", "STOCK_RESERVED");
                    responseEvent.put("hargaObat", obat.getHarga() * jumlah);
                    sagaInstanceService.updateSagaStatus(sagaId, "IN_PROGRESS", null);
                } else {
                    log.warn("❌ Insufficient stock for medicine: {}. Available: {}, Requested: {}", 
                            namaObat, obat.getStok(), jumlah);
                    responseEvent.put("status", "STOCK_FAILED");
                    responseEvent.put("hargaObat", 0.0);
                    sagaInstanceService.updateSagaStatus(sagaId, "FAILED", 
                        "Insufficient stock: " + namaObat);
                }
            } else {
                log.warn("❌ Medicine not found: {}", namaObat);
                responseEvent.put("status", "STOCK_FAILED");
                responseEvent.put("hargaObat", 0.0);
                sagaInstanceService.updateSagaStatus(sagaId, "FAILED", 
                    "Medicine not found: " + namaObat);
            }

            kafkaTemplate.send(KafkaConfig.PRESCRIPTION_RECEIVED_TOPIC, prescriptionId.toString(), responseEvent);
            log.info("Published prescription-received event for prescriptionId: {} with status: {}", 
                    prescriptionId, responseEvent.get("status"));

        } catch (Exception e) {
            log.error("❌ Error processing prescription-created event with messageId: {}", messageId, e);
            if (sagaInstance != null) {
                sagaInstanceService.markSagaFailed(sagaInstance.getSagaId(), 
                    "Exception: " + e.getMessage());
            }
            // Send to DLQ on error
            try {
                event.put("messageId", messageId);
                event.put("dlqReason", "Error in listenPrescriptionCreated: " + e.getMessage());
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
     * Listen to payment-processed events for compensation or completion
     * Implements: Compensation Logic + Idempotency + State Tracking
     */
    @KafkaListener(topics = "payment-processed", groupId = "pharmacy-saga-group")
    @Transactional
    public void listenPaymentProcessed(
            Map<String, Object> event, 
            Acknowledgment ack,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String messageKey) {
        
        String messageId = messageKey != null ? messageKey : UUID.randomUUID().toString();
        log.info("Received payment-processed event in pharmacy-service with messageId: {}", messageId);
        
        SagaInstance sagaInstance = null;
        try {
            // ✅ IDEMPOTENCY: Check if message already processed
            if (sagaInstanceService.isMessageAlreadyProcessed(messageId)) {
                log.warn("⚠️ IDEMPOTENCY: Message {} already processed, skipping", messageId);
                ack.acknowledge();
                return;
            }

            String status = (String) event.get("status");
            Long prescriptionId = Long.valueOf(event.get("resepId").toString());
            String namaObat = (String) event.get("namaObat");
            Integer jumlah = Integer.valueOf(event.get("jumlah").toString());
            String sagaId = (String) event.getOrDefault("sagaId", "unknown");

            // ✅ TRACK SAGA INSTANCE
            sagaInstance = sagaInstanceService.getOrCreateSagaInstance(
                messageId,
                "payment-processed",
                event
            );

            if ("FAILED".equals(status)) {
                log.warn("⚠️ Payment failed for prescription: {}, executing compensation", prescriptionId);
                
                // COMPENSATION: Restore stock!
                Optional<Obat> optionalObat = obatRepository.findByNamaObat(namaObat);
                if (optionalObat.isPresent()) {
                    Obat obat = optionalObat.get();
                    obat.setStok(obat.getStok() + jumlah);
                    obatRepository.save(obat);
                    log.info("✅ COMPENSATION EXECUTED: Reverted stock for prescriptionId: {}, medicine: {}, new stock: {}", 
                            prescriptionId, namaObat, obat.getStok());
                    
                    sagaInstanceService.markCompensationExecuted(sagaId);
                } else {
                    log.error("❌ COMPENSATION ERROR: Could not find medicine {} to revert stock", namaObat);
                    sagaInstanceService.markSagaFailed(sagaId, 
                        "Compensation failed: Medicine not found");
                }
            } else if ("SUCCESS".equals(status)) {
                log.info("✅ Saga completed successfully for prescriptionId: {}. Stock reservation finalized.", prescriptionId);
                sagaInstanceService.updateSagaStatus(sagaId, "COMPLETED", null);
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
