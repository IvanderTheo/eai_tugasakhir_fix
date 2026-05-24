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
@Schema(description = "Authentication response with JWT")
public class AuthTokenResponse {

    @Schema(description = "JWT access token")
    private String token;

    @Schema(example = "admin")
    private String username;

    @Schema(example = "Login successful")
    private String message;
}
