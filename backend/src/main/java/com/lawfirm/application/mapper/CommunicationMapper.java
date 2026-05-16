package com.lawfirm.application.mapper;

import com.lawfirm.application.dto.response.CommunicationResponse;
import com.lawfirm.domain.model.Communication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommunicationMapper {

    @Mapping(target = "caseId",        source = "caseEntity.id")
    @Mapping(target = "caseNumber",    source = "caseEntity.fullCaseNumber")
    @Mapping(target = "sentByUsername", source = "sentBy.username")
    CommunicationResponse toResponse(Communication communication);
}
