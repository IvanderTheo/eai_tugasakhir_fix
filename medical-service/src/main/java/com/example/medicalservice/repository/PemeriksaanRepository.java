package com.example.medicalservice.repository;

import com.example.medicalservice.entity.Pemeriksaan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PemeriksaanRepository extends JpaRepository<Pemeriksaan, Long> {
    List<Pemeriksaan> findByPasienId(Long pasienId);
    List<Pemeriksaan> findByDokterIdOrderByTanggalPemeriksaanDesc(String dokterId);
}

