package com.lawfirm.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConflictCheckRequest(
    @NotBlank @Size(max = 255) String searchName
) {}
