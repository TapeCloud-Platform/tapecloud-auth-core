package com.tapecloud.auth.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** El login acepta email o nombre de usuario indistintamente, por eso no valida formato de email. */
public record LoginRequest(
        @NotBlank(message = "Ingresá tu email o usuario")
        String identifier,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
        String password,

        /** Solo requerido si la cuenta tiene activada la verificación en dos pasos. */
        String totpCode
) {
}
