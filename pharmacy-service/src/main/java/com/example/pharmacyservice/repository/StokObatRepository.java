package com.example.pharmacyservice.repository;

import com.example.pharmacyservice.entity.StokObat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StokObatRepository extends JpaRepository<StokObat, Long> {
    List<StokObat> findByObatIdOrderByTanggalTransaksiDesc(Long obatId);
}

