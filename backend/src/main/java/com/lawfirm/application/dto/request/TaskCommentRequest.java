package com.lawfirm.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TaskCommentRequest(
    @NotBlank String content
) {}
