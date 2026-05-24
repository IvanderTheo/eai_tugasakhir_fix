package com.example.paymentservice.repository;

import com.example.paymentservice.entity.Tagihan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagihanRepository extends JpaRepository<Tagihan, Long> {
    List<Tagihan> findByPasienId(Long pasienId);
    List<Tagihan> findByStatus(String status);
    Optional<Tagihan> findByNoInvoice(String noInvoice);
    Optional<Tagihan> findByResepId(Long resepId);
}

