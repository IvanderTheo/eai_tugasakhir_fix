package com.example.paymentservice.messaging;

import com.example.paymentservice.config.KafkaConfig;
import com.example.paymentservice.entity.SagaInstance;
import com.example.paymentservice.entity.Tagihan;
import com.example.paymentservice.service.SagaInstanceService;
import com.example.paymentservice.service.TagihanService;
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
import java.util.UUID;

@Component
@Slf4j
public class SagaPaymentListener {

    @Autowired
    private TagihanService tagihanService;
    
    @Autowired
    private SagaInstanceService sagaInstanceService;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Listen to prescription-received events and create invoice/billing
     * Implements: Idempotency + State Tracking + Payment Processing
     */
    @KafkaListener(topics = KafkaConfig.PRESCRIPTION_RECEIVED_TOPIC, groupId = "payment-saga-group")
    @Transactional
    public void listenPrescriptionReceived(
            Map<String, Object> event, 
            Acknowledgment ack,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String messageKey) {
        
        String messageId = messageKey != null ? messageKey : UUID.randomUUID().toString();
        log.info("Received prescription-received event in payment-service with messageId: {}", messageId);
        
        SagaInstance sagaInstance = null;
        try {
            // ✅ IDEMPOTENCY: Check if message already processed
            if (sagaInstanceService.isMessageAlreadyProcessed(messageId)) {
                log.warn("⚠️ IDEMPOTENCY: Message {} already processed, skipping", messageId);
                ack.acknowledge();
                return;
            }

            String status = (String) event.get("status");
            String sagaId = (String) event.getOrDefault("sagaId", UUID.randomUUID().toString());

            // ✅ TRACK SAGA INSTANCE
            sagaInstance = sagaInstanceService.getOrCreateSagaInstance(
                messageId,
                "prescription-received",
                event
            );

            if ("STOCK_RESERVED".equals(status)) {
                Long prescriptionId = Long.valueOf(event.get("prescriptionId").toString());
                Long patientId = Long.valueOf(event.get("pasienId").toString());
                Double hargaObat = Double.valueOf(event.get("hargaObat").toString());
                String namaObat = (String) event.get("namaObat");
                Integer jumlah = Integer.valueOf(event.get("jumlah").toString());

                log.info("✅ Stock reserved for prescription: {}. Creating invoice...", prescriptionId);
                
                Tagihan tagihan = new Tagihan();
                tagihan.setPasienId(patientId);
                tagihan.setResepId(prescriptionId);
                tagihan.setNamaObat(namaObat);
                tagihan.setJumlahObat(jumlah);
                tagihan.setBiayaKonsultasi(50000.0); // Standard consultation fee
                tagihan.setHargaObat(hargaObat);
                tagihan.setDiskonAsuransi(0.0);
                tagihan.setStatus("PENDING");

                tagihanService.createTagihan(tagihan);
                sagaInstanceService.updateSagaStatus(sagaId, "IN_PROGRESS", null);
                
                // Publish payment-request event for actual payment processing
                Map<String, Object> paymentEvent = new HashMap<>();
                paymentEvent.put("resepId", prescriptionId);
                paymentEvent.put("pasienId", patientId);
                paymentEvent.put("sagaId", sagaId);
                paymentEvent.put("messageId", messageId);
                paymentEvent.put("totalAmount", tagihan.getBiayaKonsultasi() + hargaObat);
                paymentEvent.put("status", "PENDING");
                
                kafkaTemplate.send("payment-request", prescriptionId.toString(), paymentEvent);
                log.info("Published payment-request event for prescriptionId: {}", prescriptionId);
                
            } else if ("STOCK_FAILED".equals(status)) {
                log.warn("❌ Stock reservation failed for prescription. Payment saga cancelled.");
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
}
