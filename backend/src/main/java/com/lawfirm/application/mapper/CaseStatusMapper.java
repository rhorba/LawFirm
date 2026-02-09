package com.lawfirm.application.mapper;

import com.lawfirm.application.dto.response.CaseStatusResponse;
import com.lawfirm.domain.model.CaseStatus;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CaseStatusMapper {

    CaseStatusResponse toResponse(CaseStatus caseStatus);

    List<CaseStatusResponse> toResponseList(List<CaseStatus> caseStatuses);
}
