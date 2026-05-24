package com.example.paymentservice.service;

import com.example.paymentservice.config.KafkaConfig;
import com.example.paymentservice.entity.Tagihan;
import com.example.paymentservice.repository.TagihanRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class TagihanService {

    @Autowired
    private TagihanRepository tagihanRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public Tagihan createTagihan(Tagihan tagihan) {
        // Generate invoice number
        tagihan.setNoInvoice("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        
        // Calculate totals
        double subtotal = (tagihan.getBiayaKonsultasi() != null ? tagihan.getBiayaKonsultasi() : 0) +
                         (tagihan.getHargaObat() != null ? tagihan.getHargaObat() : 0);
        tagihan.setSubtotal(subtotal);

        double diskon = tagihan.getDiskonAsuransi() != null ? tagihan.getDiskonAsuransi() : 0;
        double pajak = (subtotal - diskon) * 0.1; // 10% PPN
        tagihan.setPajakPPN(pajak);
        tagihan.setTotalBayar(subtotal - diskon + pajak);

        Tagihan saved = tagihanRepository.save(tagihan);

        // Publish event to Kafka
        Map<String, Object> event = new HashMap<>();
        event.put("id", saved.getId());
        event.put("noInvoice", saved.getNoInvoice());
        event.put("pasienId", saved.getPasienId());
        event.put("totalBayar", saved.getTotalBayar());
        event.put("timestamp", System.currentTimeMillis());
        kafkaTemplate.send(KafkaConfig.BILLING_CREATED_TOPIC, saved.getId().toString(), event);

        log.info("Billing created with ID: {} and invoice: {}", saved.getId(), saved.getNoInvoice());
        return saved;
    }

    @Transactional(readOnly = true)
    public Tagihan getTagihanById(Long id) {
        Optional<Tagihan> optional = tagihanRepository.findById(id);
        if (optional.isEmpty()) {
            throw new RuntimeException("Invoice not found");
        }
        return optional.get();
    }

    @Transactional(readOnly = true)
    public List<Tagihan> getTagihanByPasienId(Long pasienId) {
        return tagihanRepository.findByPasienId(pasienId);
    }

    @Transactional(readOnly = true)
    public List<Tagihan> getPendingTagihan() {
        return tagihanRepository.findByStatus("PENDING");
    }

    @Transactional
    public Tagihan updateTagihanStatus(Long id, String status) {
        Optional<Tagihan> optional = tagihanRepository.findById(id);
        if (optional.isEmpty()) {
            throw new RuntimeException("Invoice not found");
        }

        Tagihan tagihan = optional.get();
        tagihan.setStatus(status);
        return tagihanRepository.save(tagihan);
    }

    @Transactional(readOnly = true)
    public Optional<Tagihan> getTagihanByResepId(Long resepId) {
        return tagihanRepository.findByResepId(resepId);
    }

    @Transactional
    public Tagihan cancelTagihan(Long id) {
        Optional<Tagihan> optional = tagihanRepository.findById(id);
        if (optional.isEmpty()) {
            throw new RuntimeException("Invoice not found");
        }

        Tagihan tagihan = optional.get();
        tagihan.setStatus("CANCELLED");
        Tagihan saved = tagihanRepository.save(tagihan);

        // Publish compensation event if there's an associated prescription
        if (tagihan.getResepId() != null) {
            Map<String, Object> event = new HashMap<>();
            event.put("tagihanId", saved.getId());
            event.put("resepId", saved.getResepId());
            event.put("pasienId", saved.getPasienId());
            event.put("namaObat", saved.getNamaObat());
            event.put("jumlah", saved.getJumlahObat());
            event.put("status", "FAILED");
            event.put("reason", "Cancelled by user");
            event.put("timestamp", System.currentTimeMillis());
            kafkaTemplate.send(KafkaConfig.PAYMENT_PROCESSED_TOPIC, saved.getResepId().toString(), event);
            log.info("Billing cancelled for invoice: {}, publishing compensation event", saved.getNoInvoice());
        }

        return saved;
    }
}

