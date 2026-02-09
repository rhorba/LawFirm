package com.lawfirm.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateLawyerRequest(
    @Size(max = 100)
    String firstName,

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
