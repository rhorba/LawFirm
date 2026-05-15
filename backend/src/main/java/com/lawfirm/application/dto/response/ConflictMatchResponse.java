package com.lawfirm.application.dto.response;

/**
 * A single match found during a conflict check.
 * source: CLIENT | CASE_PARTY | CASE
 */
public record ConflictMatchResponse(
    String source,
    String matchedName,
    String partyType,
    Long caseId,
    String caseNumber,
    Long clientId
) {}
