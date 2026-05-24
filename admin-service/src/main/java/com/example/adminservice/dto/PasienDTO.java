package com.example.adminservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Patient data (JSON request/response body)")
public class PasienDTO {
    private Long id;
    @Schema(example = "1234567890123456")
    private String nik;
    @Schema(example = "Budi Santoso")
    private String nama;
    @Schema(example = "RM001")
    private String noRM;
    private String alamat;
    private String noTelepon;
    private String email;
    private String jenisKelamin;
    private String tanggalLahir;
}

