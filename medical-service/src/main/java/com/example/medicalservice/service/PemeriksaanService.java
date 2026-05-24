package com.example.medicalservice.service;

import com.example.medicalservice.config.KafkaConfig;
import com.example.medicalservice.entity.Pemeriksaan;
import com.example.medicalservice.repository.PemeriksaanRepository;
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
public class PemeriksaanService {

    @Autowired
    private PemeriksaanRepository pemeriksaanRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public Pemeriksaan createPemeriksaan(Pemeriksaan pemeriksaan) {
        Pemeriksaan saved = pemeriksaanRepository.save(pemeriksaan);
        
        // Publish event to Kafka
        Map<String, Object> event = new HashMap<>();
        event.put("id", saved.getId());
        event.put("pasienId", saved.getPasienId());
        event.put("timestamp", System.currentTimeMillis());
        kafkaTemplate.send(KafkaConfig.MEDICAL_RECORD_CREATED_TOPIC, saved.getId().toString(), event);
        
        log.info("Medical record created with ID: {} for patient: {}", saved.getId(), saved.getPasienId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Pemeriksaan getPemeriksaanById(Long id) {
        Optional<Pemeriksaan> optional = pemeriksaanRepository.findById(id);
        if (optional.isEmpty()) {
            log.warn("Medical record not found with id: {}", id);
            throw new RuntimeException("Medical record not found");
        }
        return optional.get();
    }

    @Transactional(readOnly = true)
    public List<Pemeriksaan> getPemeriksaanByPasienId(Long pasienId) {
        return pemeriksaanRepository.findByPasienId(pasienId);
    }

    @Transactional
    public Pemeriksaan updatePemeriksaan(Long id, Pemeriksaan pemeriksaan) {
        Optional<Pemeriksaan> optional = pemeriksaanRepository.findById(id);
        if (optional.isEmpty()) {
            throw new RuntimeException("Medical record not found");
        }

        Pemeriksaan existing = optional.get();
        existing.setTekananDarah(pemeriksaan.getTekananDarah());
        existing.setBeratBadan(pemeriksaan.getBeratBadan());
        existing.setTinggiBadan(pemeriksaan.getTinggiBadan());
        existing.setSuhuTubuh(pemeriksaan.getSuhuTubuh());
        existing.setKeluhan(pemeriksaan.getKeluhan());
        existing.setHasilPemeriksaan(pemeriksaan.getHasilPemeriksaan());

        return pemeriksaanRepository.save(existing);
    }
}

