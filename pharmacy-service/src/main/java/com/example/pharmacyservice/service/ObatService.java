package com.example.pharmacyservice.service;

import com.example.pharmacyservice.config.KafkaConfig;
import com.example.pharmacyservice.entity.Obat;
import com.example.pharmacyservice.repository.ObatRepository;
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
public class ObatService {

    @Autowired
    private ObatRepository obatRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public Obat createObat(Obat obat) {
        Obat saved = obatRepository.save(obat);
        log.info("Medicine created with ID: {}", saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Obat getObatById(Long id) {
        Optional<Obat> optional = obatRepository.findById(id);
        if (optional.isEmpty()) {
            throw new RuntimeException("Medicine not found");
        }
        return optional.get();
    }

    @Transactional(readOnly = true)
    public List<Obat> getAllObat() {
        return obatRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Obat> searchObat(String nama) {
        return obatRepository.findByNamaObatContainingIgnoreCase(nama);
    }

    @Transactional(readOnly = true)
    public List<Obat> getLowStockMedicines() {
        return obatRepository.findByStokLessThanEqual(10);
    }

    @Transactional
    public Obat updateStok(Long id, Integer newStok) {
        Optional<Obat> optional = obatRepository.findById(id);
        if (optional.isEmpty()) {
            throw new RuntimeException("Medicine not found");
        }

        Obat obat = optional.get();
        obat.setStok(newStok);
        Obat updated = obatRepository.save(obat);

        // Check if stock is low and publish alert
        if (newStok <= obat.getStokMinimal()) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("obatId", obat.getId());
            alert.put("namaObat", obat.getNamaObat());
            alert.put("stokSaat", newStok);
            alert.put("stokMinimal", obat.getStokMinimal());
            kafkaTemplate.send(KafkaConfig.STOCK_LOW_ALERT_TOPIC, obat.getId().toString(), alert);
            log.warn("Low stock alert for medicine: {}", obat.getNamaObat());
        }

        // Publish stock update event
        Map<String, Object> event = new HashMap<>();
        event.put("obatId", obat.getId());
        event.put("stok", newStok);
        kafkaTemplate.send(KafkaConfig.STOCK_UPDATED_TOPIC, obat.getId().toString(), event);

        return updated;
    }
}

