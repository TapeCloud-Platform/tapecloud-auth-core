package com.tapecloud.auth.review.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 150, message = "El título no puede superar los 150 caracteres")
        String title,

        @NotBlank(message = "La opinión/cuerpo es obligatoria")
        @Size(max = 750, message = "La reseña no puede superar los 750 caracteres")
        String body,

        @NotNull(message = "La puntuación es obligatoria")
        @DecimalMin(value = "0.5", message = "La puntuación mínima es 0.5")
        @DecimalMax(value = "5.0", message = "La puntuación máxima es 5")
        Double rating,

        Boolean isSpoiler
) {
}
