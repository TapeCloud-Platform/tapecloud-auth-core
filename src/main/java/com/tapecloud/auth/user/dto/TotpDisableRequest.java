package com.tapecloud.auth.user.dto;

import jakarta.validation.constraints.NotBlank;

public record TotpDisableRequest(
        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {
}
