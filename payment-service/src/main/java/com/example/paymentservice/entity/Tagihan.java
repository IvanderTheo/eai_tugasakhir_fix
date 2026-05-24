package com.example.paymentservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tagihan")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tagihan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long pasienId;

    @Column
    private Long resepId;

    @Column
    private String namaObat;

    @Column
    private Integer jumlahObat;

    @Column
    private Double biayaKonsultasi;

    @Column
    private Double hargaObat;

    @Column
    private Double subtotal;

    @Column
    private Double diskonAsuransi;

    @Column
    private Double pajakPPN;

    @Column
    private Double totalBayar;

    @Column
    private String status;

    @Column
    private String noInvoice;

    @Column(name = "tanggal_tagihan")
    private LocalDateTime tanggalTagihan;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        tanggalTagihan = LocalDateTime.now();
        status = "PENDING";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

