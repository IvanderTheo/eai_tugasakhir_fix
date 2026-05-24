package com.example.medicalservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "pemeriksaan")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pemeriksaan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long pasienId;

    @Column
    private String tekananDarah;

    @Column
    private Double beratBadan;

    @Column
    private Double tinggiBadan;

    @Column
    private Double suhuTubuh;

    @Column(columnDefinition = "TEXT")
    private String keluhan;

    @Column(columnDefinition = "TEXT")
    private String hasilPemeriksaan;

    @Column
    private String dokterId;

    @Column(name = "tanggal_pemeriksaan")
    private LocalDateTime tanggalPemeriksaan;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        tanggalPemeriksaan = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

