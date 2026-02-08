package com.boilerplate.application.mapper;

import com.boilerplate.application.dto.response.CaseStatusResponse;
import com.boilerplate.domain.model.CaseStatus;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CaseStatusMapper {

    CaseStatusResponse toResponse(CaseStatus caseStatus);

    List<CaseStatusResponse> toResponseList(List<CaseStatus> caseStatuses);
}
