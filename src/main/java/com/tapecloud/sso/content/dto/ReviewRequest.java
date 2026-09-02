package com.tapecloud.sso.content.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
        @NotBlank @Size(max = 180) String title,
        @NotBlank @Size(max = 5000) String body,
        @NotNull @Min(1) @Max(5) Integer rating
) {
}
