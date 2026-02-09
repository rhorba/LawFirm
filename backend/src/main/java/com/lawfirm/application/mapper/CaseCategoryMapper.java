package com.lawfirm.application.mapper;

import com.lawfirm.application.dto.response.CaseCategoryResponse;
import com.lawfirm.domain.model.CaseCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CaseCategoryMapper {

    @Mapping(target = "caseTypeCode", source = "caseType.code")
    CaseCategoryResponse toResponse(CaseCategory caseCategory);

    List<CaseCategoryResponse> toResponseList(List<CaseCategory> caseCategories);
}
