package com.tapecloud.auth.user.dto;

import java.util.List;

public record AuthResponse(
        String token,
        String email,
        String username,
        String displayName,
        List<String> roles
) {
}
