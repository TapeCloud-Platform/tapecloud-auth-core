package com.tapecloud.auth.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank(message = "El comentario no puede estar vacío")
        @Size(max = 2000, message = "El comentario no puede superar los 2000 caracteres")
        String body
) {
}
