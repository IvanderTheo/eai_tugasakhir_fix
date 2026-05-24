package com.example.pharmacyservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "obat")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Obat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String kodeObat;

    @Column(nullable = false)
    private String namaObat;

    @Column
    private String deskripsi;

    @Column
    private Double harga;

    @Column
    private String satuan;

    @Column
    private Integer stok;

    @Column
    private Integer stokMinimal;

    @Column
    private String supplier;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

