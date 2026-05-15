package com.lawfirm.application.mapper;

import com.lawfirm.application.dto.response.CalendarEventResponse;
import com.lawfirm.domain.model.CalendarEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CalendarEventMapper {

    @Mapping(target = "caseId",           source = "caseEntity.id")
    @Mapping(target = "caseNumber",       source = "caseEntity.fullCaseNumber")
    @Mapping(target = "createdByUserId",  source = "createdBy.id")
    @Mapping(target = "createdByUsername", source = "createdBy.username")
    CalendarEventResponse toResponse(CalendarEvent event);

    List<CalendarEventResponse> toResponseList(List<CalendarEvent> events);
}
