package com.example.paymentservice.repository;

import com.example.paymentservice.entity.Transaksi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransaksiRepository extends JpaRepository<Transaksi, Long> {
    List<Transaksi> findByTagihanId(Long tagihanId);
    List<Transaksi> findByStatusPembayaran(String statusPembayaran);
}

