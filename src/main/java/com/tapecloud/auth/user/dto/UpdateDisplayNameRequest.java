package com.tapecloud.auth.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateDisplayNameRequest(
        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(min = 2, max = 60, message = "El nombre debe tener entre 2 y 60 caracteres")
        String displayName
) {
}
