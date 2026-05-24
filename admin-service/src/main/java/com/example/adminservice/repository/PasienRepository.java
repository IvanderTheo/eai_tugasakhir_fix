package com.example.adminservice.repository;

import com.example.adminservice.entity.Pasien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PasienRepository extends JpaRepository<Pasien, Long> {
    Optional<Pasien> findByNik(String nik);
    Optional<Pasien> findByNoRM(String noRM);
    List<Pasien> findByNamaContainingIgnoreCase(String nama);
}

