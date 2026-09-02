package com.tapecloud.sso.content.dto;

import com.tapecloud.sso.content.entity.ContentApp;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContentRequest(
        @NotNull ContentApp app,
        @NotBlank @Size(max = 40) String type,
        @NotBlank @Size(max = 240) String title,
        @NotBlank @Size(max = 3000) String description,
        @NotBlank @Size(max = 120) String genre,
        Integer releaseYear
) {
}
