package com.lawfirm.application.dto.request;

import com.lawfirm.domain.model.ConflictParty.PartyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConflictPartyRequest(
    @NotNull PartyType partyType,
    @NotBlank @Size(max = 255) String name,
    String notes
) {}
