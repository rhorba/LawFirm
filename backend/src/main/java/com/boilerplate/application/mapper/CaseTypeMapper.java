package com.boilerplate.application.mapper;

import com.boilerplate.application.dto.response.CaseTypeResponse;
import com.boilerplate.domain.model.CaseType;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {CaseStatusMapper.class})
public interface CaseTypeMapper {

    CaseTypeResponse toResponse(CaseType caseType);

    List<CaseTypeResponse> toResponseList(List<CaseType> caseTypes);
}
