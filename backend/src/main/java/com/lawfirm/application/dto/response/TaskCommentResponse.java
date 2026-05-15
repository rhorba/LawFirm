package com.lawfirm.application.dto.response;

import java.time.LocalDateTime;

public record TaskCommentResponse(
    Long id,
    Long taskId,
    Long authorUserId,
    String authorUsername,
    String content,
    LocalDateTime createdAt
) {}
