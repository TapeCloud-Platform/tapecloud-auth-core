package com.tapecloud.auth.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TotpEnableRequest(
        @NotBlank(message = "El código es obligatorio")
        @Pattern(regexp = "^[0-9]{6}$", message = "El código debe tener 6 dígitos")
        String code
) {
}
