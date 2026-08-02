package com.caltalk.backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 64) String password,
        @NotBlank @Size(min = 8, max = 64) String passwordConfirmation
) {

    public SignupRequest {
        email = email == null ? null : email.trim();
    }
}
