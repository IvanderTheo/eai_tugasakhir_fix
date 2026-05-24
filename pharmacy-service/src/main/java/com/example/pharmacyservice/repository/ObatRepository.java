package com.example.pharmacyservice.repository;

import com.example.pharmacyservice.entity.Obat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ObatRepository extends JpaRepository<Obat, Long> {
    Optional<Obat> findByKodeObat(String kodeObat);
    Optional<Obat> findByNamaObat(String namaObat);
    List<Obat> findByNamaObatContainingIgnoreCase(String namaObat);
    List<Obat> findByStokLessThanEqual(Integer stokMinimal);
}

