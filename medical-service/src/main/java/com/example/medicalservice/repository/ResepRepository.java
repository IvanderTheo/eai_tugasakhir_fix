package com.example.medicalservice.repository;

import com.example.medicalservice.entity.Resep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResepRepository extends JpaRepository<Resep, Long> {
    List<Resep> findByPasienId(Long pasienId);
    List<Resep> findByPemeriksaanId(Long pemeriksaanId);
    List<Resep> findByStatus(String status);
}

