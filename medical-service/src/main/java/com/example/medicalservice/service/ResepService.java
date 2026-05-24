package com.example.medicalservice.service;

import com.example.medicalservice.config.KafkaConfig;
import com.example.medicalservice.entity.Resep;
import com.example.medicalservice.repository.ResepRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class ResepService {

    @Autowired
    private ResepRepository resepRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public Resep createResep(Resep resep) {
        Resep saved = resepRepository.save(resep);
        
        // Publish event to Kafka
        Map<String, Object> event = new HashMap<>();
        event.put("id", saved.getId());
        event.put("pasienId", saved.getPasienId());
        event.put("namaObat", saved.getNamaObat());
        event.put("jumlah", saved.getJumlah() != null ? saved.getJumlah() : 1);
        event.put("timestamp", System.currentTimeMillis());
        kafkaTemplate.send(KafkaConfig.PRESCRIPTION_CREATED_TOPIC, saved.getId().toString(), event);
        
        log.info("Prescription created with ID: {} for patient: {}", saved.getId(), saved.getPasienId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Resep getResepById(Long id) {
        Optional<Resep> optional = resepRepository.findById(id);
        if (optional.isEmpty()) {
            log.warn("Prescription not found with id: {}", id);
            throw new RuntimeException("Prescription not found");
        }
        return optional.get();
    }

    @Transactional(readOnly = true)
    public List<Resep> getResepByPasienId(Long pasienId) {
        return resepRepository.findByPasienId(pasienId);
    }

    @Transactional(readOnly = true)
    public List<Resep> getPendingResep() {
        return resepRepository.findByStatus("PENDING");
    }

    @Transactional
    public Resep updateResepStatus(Long id, String status) {
        Optional<Resep> optional = resepRepository.findById(id);
        if (optional.isEmpty()) {
            throw new RuntimeException("Prescription not found");
        }

        Resep resep = optional.get();
        resep.setStatus(status);
        return resepRepository.save(resep);
    }
}

