package com.lawfirm.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EmailSendRequest(
    @NotNull @Email String to,
    @NotBlank String subject,
    @NotBlank String body
) {}
