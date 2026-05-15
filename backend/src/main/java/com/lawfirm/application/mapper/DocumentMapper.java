package com.lawfirm.application.mapper;

import com.lawfirm.application.dto.response.DocumentResponse;
import com.lawfirm.domain.model.Document;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(target = "caseId",            source = "caseEntity.id")
    @Mapping(target = "caseNumber",        source = "caseEntity.fullCaseNumber")
    @Mapping(target = "uploadedByUsername", source = "uploadedBy.username")
    DocumentResponse toResponse(Document document);
}
