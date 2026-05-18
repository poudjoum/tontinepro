package com.tontinepro.tontinepro_backend.api.auth;

import com.tontinepro.tontinepro_backend.api.auth.dto.AuthResponse;

/**
 * Résultat du login : soit des tokens complets, soit un challenge 2FA.
 */
public sealed interface LoginResult permits LoginResult.Authenticated, LoginResult.TwoFaRequired {

    record Authenticated(AuthResponse auth) implements LoginResult {}

    record TwoFaRequired(String ticket, String email) implements LoginResult {}
}
