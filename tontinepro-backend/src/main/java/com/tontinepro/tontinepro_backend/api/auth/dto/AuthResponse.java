package com.tontinepro.tontinepro_backend.api.auth.dto;

public record AuthResponse(

        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        String email,
        String role,
        boolean twoFaEnabled,
        boolean mustChangePassword
) {
    public static AuthResponse of(String accessToken, String refreshToken,
                                  long expiresIn, String email, String role,
                                  boolean twoFaEnabled, boolean mustChangePassword) {
        return new AuthResponse(accessToken, refreshToken, "Bearer",
                expiresIn, email, role, twoFaEnabled, mustChangePassword);
    }
}
