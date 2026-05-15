package com.lawfirm.application.dto.response;

import com.lawfirm.domain.model.ConflictParty.PartyType;

import java.time.LocalDateTime;

public record ConflictPartyResponse(
    Long id,
    Long caseId,
    String caseNumber,
    PartyType partyType,
    String name,
    String notes,
    LocalDateTime createdAt
) {}
