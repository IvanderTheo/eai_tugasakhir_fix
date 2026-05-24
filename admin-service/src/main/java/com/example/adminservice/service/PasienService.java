package com.example.adminservice.service;

import com.example.adminservice.config.KafkaConfig;
import com.example.adminservice.dto.PasienDTO;
import com.example.adminservice.entity.Pasien;
import com.example.adminservice.repository.PasienRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PasienService {

    @Autowired
    private PasienRepository pasienRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public PasienDTO createPasien(PasienDTO dto) {
        Pasien pasien = Pasien.builder()
                .nik(dto.getNik())
                .nama(dto.getNama())
                .noRM(dto.getNoRM())
                .alamat(dto.getAlamat())
                .noTelepon(dto.getNoTelepon())
                .email(dto.getEmail())
                .jenisKelamin(dto.getJenisKelamin())
                .tanggalLahir(dto.getTanggalLahir())
                .build();

        Pasien savedPasien = pasienRepository.save(pasien);
        
        // Publish event to Kafka
        kafkaTemplate.send(KafkaConfig.PATIENT_CREATED_TOPIC, savedPasien.getId().toString(), savedPasien);
        log.info("Pasien created with ID: {} and published to Kafka", savedPasien.getId());

        return convertToDTO(savedPasien);
    }

    @Transactional
    public PasienDTO updatePasien(Long id, PasienDTO dto) {
        Optional<Pasien> optionalPasien = pasienRepository.findById(id);
        if (optionalPasien.isEmpty()) {
            log.warn("Pasien with ID {} not found", id);
            throw new RuntimeException("Pasien not found with id: " + id);
        }

        Pasien pasien = optionalPasien.get();
        pasien.setNama(dto.getNama());
        pasien.setAlamat(dto.getAlamat());
        pasien.setNoTelepon(dto.getNoTelepon());
        pasien.setEmail(dto.getEmail());
        pasien.setJenisKelamin(dto.getJenisKelamin());
        pasien.setTanggalLahir(dto.getTanggalLahir());

        Pasien updatedPasien = pasienRepository.save(pasien);
        
        // Publish update event to Kafka
        kafkaTemplate.send(KafkaConfig.PATIENT_UPDATED_TOPIC, updatedPasien.getId().toString(), updatedPasien);
        log.info("Pasien updated with ID: {} and published to Kafka", updatedPasien.getId());

        return convertToDTO(updatedPasien);
    }

    @Transactional(readOnly = true)
    public PasienDTO getPasienById(Long id) {
        Optional<Pasien> optionalPasien = pasienRepository.findById(id);
        if (optionalPasien.isEmpty()) {
            log.warn("Pasien with ID {} not found", id);
            throw new RuntimeException("Pasien not found with id: " + id);
        }
        return convertToDTO(optionalPasien.get());
    }

    @Transactional(readOnly = true)
    public PasienDTO getPasienByNik(String nik) {
        Optional<Pasien> optionalPasien = pasienRepository.findByNik(nik);
        if (optionalPasien.isEmpty()) {
            log.warn("Pasien with NIK {} not found", nik);
            throw new RuntimeException("Pasien not found with NIK: " + nik);
        }
        return convertToDTO(optionalPasien.get());
    }

    @Transactional(readOnly = true)
    public List<PasienDTO> getAllPasien() {
        return pasienRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PasienDTO> searchPasienByNama(String nama) {
        return pasienRepository.findByNamaContainingIgnoreCase(nama).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deletePasien(Long id) {
        if (!pasienRepository.existsById(id)) {
            log.warn("Pasien with ID {} not found", id);
            throw new RuntimeException("Pasien not found with id: " + id);
        }
        pasienRepository.deleteById(id);
        log.info("Pasien deleted with ID: {}", id);
    }

    private PasienDTO convertToDTO(Pasien pasien) {
        return PasienDTO.builder()
                .id(pasien.getId())
                .nik(pasien.getNik())
                .nama(pasien.getNama())
                .noRM(pasien.getNoRM())
                .alamat(pasien.getAlamat())
                .noTelepon(pasien.getNoTelepon())
                .email(pasien.getEmail())
                .jenisKelamin(pasien.getJenisKelamin())
                .tanggalLahir(pasien.getTanggalLahir())
                .build();
    }
}

