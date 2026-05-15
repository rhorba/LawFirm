package com.lawfirm.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ConflictClearRequest(
    @NotBlank String clearedNote
) {}
