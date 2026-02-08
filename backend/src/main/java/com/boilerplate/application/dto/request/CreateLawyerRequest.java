package com.boilerplate.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLawyerRequest(
    @NotBlank(message = "First name is required")
    @Size(max = 100)
    String firstName,

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    String lastName,

    @Size(max = 50)
    String taxId,

    @Email
    @Size(max = 100)
    String email,

    @Size(max = 20)
    String phone
) {}
