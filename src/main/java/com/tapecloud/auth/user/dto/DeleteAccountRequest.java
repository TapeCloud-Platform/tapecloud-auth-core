package com.tapecloud.auth.user.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(
        @NotBlank(message = "La contraseña es obligatoria para eliminar la cuenta")
        String password,

        /** Código 2FA (solo si la cuenta lo tiene activado). */
        String totpCode,

        /** Código enviado por email (solo si la cuenta no tiene 2FA). */
        String emailCode
) {
}
