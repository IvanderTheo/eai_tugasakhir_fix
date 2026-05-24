package com.example.medicalservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "resep")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long pemeriksaanId;

    @Column(nullable = false)
    private Long pasienId;

    @Column
    private String namaObat;

    @Column
    private String dosis;

    @Column
    private String frekuensi;

    @Column
    private Integer jumlah;

    @Column(columnDefinition = "TEXT")
    private String catatan;

    @Column
    private String status;

    @Column
    private String dokterNama;

    @Column(name = "tanggal_resep")
    private LocalDateTime tanggalResep;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        tanggalResep = LocalDateTime.now();
        status = "PENDING";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

