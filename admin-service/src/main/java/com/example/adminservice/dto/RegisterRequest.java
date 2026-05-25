package com.example.adminservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User registration (JSON)")
public class RegisterRequest {

    @Schema(example = "dokter1")
    @NotBlank
    private String username;

    @Schema(example = "pass123")
    @NotBlank
    private String password;

    @Schema(example = "Dr. Budi")
    @NotBlank
    private String nama;

    @Schema(example = "budi@test.com")
    @NotBlank
    private String email;

    @Schema(example = "DOCTOR", allowableValues = {"ADMIN", "DOCTOR", "PHARMACIST", "STAFF", "USER"})
    private String role = "USER";
}
