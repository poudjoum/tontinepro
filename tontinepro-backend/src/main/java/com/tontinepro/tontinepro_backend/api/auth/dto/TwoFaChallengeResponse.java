package com.tontinepro.tontinepro_backend.api.auth.dto;

public record TwoFaChallengeResponse(

        boolean requires2fa,

        /** Ticket éphémère (5 min) à passer avec le code TOTP sur /2fa/valider. */
        String ticket,

        String email
) {}
