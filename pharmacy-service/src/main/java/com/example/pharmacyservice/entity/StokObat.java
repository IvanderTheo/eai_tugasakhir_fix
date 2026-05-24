package com.example.pharmacyservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "stok_obat")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StokObat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long obatId;

    @Column
    private Integer jumlahMasuk;

    @Column
    private Integer jumlahKeluar;

    @Column
    private String tipe;

    @Column
    private String keterangan;

    @Column(name = "tanggal_transaksi")
    private LocalDateTime tanggalTransaksi;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        tanggalTransaksi = LocalDateTime.now();
    }
}

