package com.boilerplate.application.mapper;

import com.boilerplate.application.dto.response.CaseCategoryResponse;
import com.boilerplate.domain.model.CaseCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CaseCategoryMapper {

    @Mapping(target = "caseTypeCode", source = "caseType.code")
    CaseCategoryResponse toResponse(CaseCategory caseCategory);

    List<CaseCategoryResponse> toResponseList(List<CaseCategory> caseCategories);
}
