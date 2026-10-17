package com.pulsepass.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Identifier is required")
        @Size(max = 255, message = "Identifier cannot exceed 255 characters")
        String identifier,

        @NotBlank(message = "Password is required")
        @Size(max = 72, message = "Password cannot exceed 72 characters")
        String password
) {
    @Override
    public String toString() {
        return "LoginRequest{identifier='" + identifier + "', password=[PROTECTED]}";
    }
}
