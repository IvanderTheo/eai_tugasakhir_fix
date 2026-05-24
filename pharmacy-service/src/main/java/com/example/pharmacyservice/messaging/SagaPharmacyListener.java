package com.example.pharmacyservice.messaging;

import com.example.pharmacyservice.config.KafkaConfig;
import com.example.pharmacyservice.entity.Obat;
import com.example.pharmacyservice.repository.ObatRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class SagaPharmacyListener {

    @Autowired
    private ObatRepository obatRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "prescription-created", groupId = "pharmacy-saga-group")
    @Transactional
    public void listenPrescriptionCreated(Map<String, Object> event, Acknowledgment ack) {
        log.info("Received prescription-created event in pharmacy-service: {}", event);
        try {
            Long prescriptionId = Long.valueOf(event.get("id").toString());
            Long patientId = Long.valueOf(event.get("pasienId").toString());
            String namaObat = (String) event.get("namaObat");
            Integer jumlah = Integer.valueOf(event.get("jumlah").toString());

            Optional<Obat> optionalObat = obatRepository.findByNamaObat(namaObat);
            
            Map<String, Object> responseEvent = new HashMap<>();
            responseEvent.put("prescriptionId", prescriptionId);
            responseEvent.put("pasienId", patientId);
            responseEvent.put("namaObat", namaObat);
            responseEvent.put("jumlah", jumlah);

            if (optionalObat.isPresent()) {
                Obat obat = optionalObat.get();
                if (obat.getStok() >= jumlah) {
                    // Reserve stock
                    obat.setStok(obat.getStok() - jumlah);
                    obatRepository.save(obat);
                    log.info("Stock reserved for prescriptionId: {}, medicine: {}, remaining stock: {}", 
                            prescriptionId, namaObat, obat.getStok());

                    responseEvent.put("status", "STOCK_RESERVED");
                    responseEvent.put("hargaObat", obat.getHarga() * jumlah);
                } else {
                    log.warn("Insufficient stock for medicine: {}. Available: {}, Requested: {}", 
                            namaObat, obat.getStok(), jumlah);
                    responseEvent.put("status", "STOCK_FAILED");
                    responseEvent.put("hargaObat", 0.0);
                }
            } else {
                log.warn("Medicine not found: {}", namaObat);
                responseEvent.put("status", "STOCK_FAILED");
                responseEvent.put("hargaObat", 0.0);
            }

            kafkaTemplate.send(KafkaConfig.PRESCRIPTION_RECEIVED_TOPIC, prescriptionId.toString(), responseEvent);
            log.info("Published prescription-received event for prescriptionId: {} with status: {}", 
                    prescriptionId, responseEvent.get("status"));

        } catch (Exception e) {
            log.error("Error processing prescription-created event", e);
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = "payment-processed", groupId = "pharmacy-saga-group")
    @Transactional
    public void listenPaymentProcessed(Map<String, Object> event, Acknowledgment ack) {
        log.info("Received payment-processed event in pharmacy-service: {}", event);
        try {
            String status = (String) event.get("status");
            Long prescriptionId = Long.valueOf(event.get("resepId").toString());
            String namaObat = (String) event.get("namaObat");
            Integer jumlah = Integer.valueOf(event.get("jumlah").toString());

            if ("FAILED".equals(status)) {
                // COMPENSATION: Restore stock!
                Optional<Obat> optionalObat = obatRepository.findByNamaObat(namaObat);
                if (optionalObat.isPresent()) {
                    Obat obat = optionalObat.get();
                    obat.setStok(obat.getStok() + jumlah);
                    obatRepository.save(obat);
                    log.info("COMPENSATION EXECUTION: Reverted stock for prescriptionId: {}, medicine: {}, new stock: {}", 
                            prescriptionId, namaObat, obat.getStok());
                } else {
                    log.error("COMPENSATION ERROR: Could not find medicine {} to revert stock", namaObat);
                }
            } else if ("SUCCESS".equals(status)) {
                log.info("Saga completed successfully for prescriptionId: {}. Stock reservation finalized.", prescriptionId);
            }
        } catch (Exception e) {
            log.error("Error processing payment-processed event", e);
        } finally {
            ack.acknowledge();
        }
    }
}
