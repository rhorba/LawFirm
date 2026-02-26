package com.lawfirm.application.mapper;

import com.lawfirm.application.dto.request.CaseTemplateRequest;
import com.lawfirm.application.dto.response.CaseTemplateResponse;
import com.lawfirm.domain.model.CaseTemplate;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CaseTemplateMapper {
    CaseTemplateResponse toResponse(CaseTemplate entity);
    CaseTemplate toEntity(CaseTemplateRequest request);
}
