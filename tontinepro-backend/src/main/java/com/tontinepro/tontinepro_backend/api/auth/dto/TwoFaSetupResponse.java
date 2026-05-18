package com.tontinepro.tontinepro_backend.api.auth.dto;

public record TwoFaSetupResponse(

        /** Secret Base32 à saisir manuellement dans l'application TOTP. */
        String secret,

        /** URI otpauth:// encodée en PNG base64 — à afficher comme QR code. */
        String qrCodeDataUri
) {}
