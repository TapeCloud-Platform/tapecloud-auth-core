package com.tapecloud.auth.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateAvatarRequest(
        @Size(max = 400_000, message = "La imagen es demasiado grande")
        String avatarDataUri
) {
}
