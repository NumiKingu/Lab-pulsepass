package com.pulsepass.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterUserRequest(
        @NotBlank(message = "Username is required")
        @Size(max = 80, message = "Username cannot exceed 80 characters")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        @Size(max = 255, message = "Email cannot exceed 255 characters")
        String email,

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name cannot exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name cannot exceed 100 characters")
        String lastName,

        @Size(max = 30, message = "Phone cannot exceed 30 characters")
        String phone,

        @Size(max = 100, message = "City cannot exceed 100 characters")
        String city,

        @NotNull(message = "Birth date is required")
        LocalDate birthDate
) {
}