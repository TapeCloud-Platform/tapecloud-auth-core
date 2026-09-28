package com.tapecloud.auth.user.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(
        @NotBlank(message = "La contraseña es obligatoria para eliminar la cuenta")
        String password
) {
}
