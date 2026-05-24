package com.example.adminservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login credentials (JSON)")
public class LoginRequest {

    @Schema(example = "admin")
    private String username;

    @Schema(example = "admin123")
    private String password;
}
