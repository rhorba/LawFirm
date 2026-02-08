package com.boilerplate.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeStatusRequest(
    @NotBlank(message = "Status code is required")
    String statusCode,

    @Size(max = 500)
    String reason
) {}
