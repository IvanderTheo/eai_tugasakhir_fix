package com.example.adminservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User registration (JSON)")
public class RegisterRequest {

    @Schema(example = "dokter1")
    private String username;

    @Schema(example = "pass123")
    private String password;

    @Schema(example = "Dr. Budi")
    private String nama;

    @Schema(example = "budi@test.com")
    private String email;

    @Schema(example = "DOCTOR", allowableValues = {"ADMIN", "DOCTOR", "PHARMACIST", "STAFF", "USER"})
    private String role = "USER";
}
