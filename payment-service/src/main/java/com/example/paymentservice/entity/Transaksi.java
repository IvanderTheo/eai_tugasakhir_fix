package com.example.paymentservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "transaksi")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaksi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tagihanId;

    @Column
    private Double jumlahBayar;

    @Column
    private String metodePembayaran;

    @Column
    private String statusPembayaran;

    @Column
    private String referensiTransaksi;

    @Column(columnDefinition = "TEXT")
    private String keterangan;

    @Column(name = "tanggal_transaksi")
    private LocalDateTime tanggalTransaksi;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        tanggalTransaksi = LocalDateTime.now();
        statusPembayaran = "PENDING";
    }
}

