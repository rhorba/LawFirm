package com.lawfirm.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ConflictCheckResultResponse(
    Long checkId,
    String searchName,
    boolean hasConflict,
    List<ConflictMatchResponse> matches,
    LocalDateTime performedAt
) {}
