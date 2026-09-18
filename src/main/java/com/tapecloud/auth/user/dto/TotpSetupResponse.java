package com.tapecloud.auth.user.dto;

public record TotpSetupResponse(
        String secret,
        String qrCodeDataUri
) {
}
