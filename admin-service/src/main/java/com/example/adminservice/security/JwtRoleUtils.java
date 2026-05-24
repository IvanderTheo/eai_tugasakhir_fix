package com.example.adminservice.security;

public final class JwtRoleUtils {

    private JwtRoleUtils() {
    }

    public static String toRoleAuthority(String role) {
        if (role == null || role.isBlank()) {
            return "ROLE_USER";
        }
        String normalized = role.trim().toUpperCase();
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }
}
