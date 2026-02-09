package com.lawfirm.application.mapper;

import com.lawfirm.application.dto.response.CaseTypeResponse;
import com.lawfirm.domain.model.CaseType;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {CaseStatusMapper.class})
public interface CaseTypeMapper {

    CaseTypeResponse toResponse(CaseType caseType);

    List<CaseTypeResponse> toResponseList(List<CaseType> caseTypes);
}
