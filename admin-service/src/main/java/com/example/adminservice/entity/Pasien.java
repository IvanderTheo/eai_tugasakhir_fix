package com.example.adminservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "pasien")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pasien {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nik;

    @Column(nullable = false)
    private String nama;

    @Column(unique = true, nullable = false)
    private String noRM;

    @Column
    private String alamat;

    @Column
    private String noTelepon;

    @Column(unique = true)
    private String email;

    @Column
    private String jenisKelamin;

    @Column
    private String tanggalLahir;

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

