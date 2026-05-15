package com.lawfirm.application.mapper;

import com.lawfirm.application.dto.response.ConflictPartyResponse;
import com.lawfirm.domain.model.ConflictParty;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ConflictPartyMapper {

    @Mapping(target = "caseId",     source = "caseEntity.id")
    @Mapping(target = "caseNumber", source = "caseEntity.fullCaseNumber")
    ConflictPartyResponse toResponse(ConflictParty party);

    List<ConflictPartyResponse> toResponseList(List<ConflictParty> parties);
}
