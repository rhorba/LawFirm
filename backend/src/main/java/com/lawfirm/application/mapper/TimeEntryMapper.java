package com.lawfirm.application.mapper;

import com.lawfirm.application.dto.response.TimeEntryResponse;
import com.lawfirm.domain.model.TimeEntry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring")
public interface TimeEntryMapper {

    @Mapping(target = "caseId",         source = "caseEntity.id")
    @Mapping(target = "caseNumber",     source = "caseEntity.fullCaseNumber")
    @Mapping(target = "lawyerId",       source = "lawyer.id")
    @Mapping(target = "lawyerFullName", source = "lawyer", qualifiedByName = "lawyerName")
    @Mapping(target = "billableAmount", source = ".", qualifiedByName = "calcAmount")
    @Mapping(target = "invoiceId",      source = "invoice.id")
    TimeEntryResponse toResponse(TimeEntry entry);

    List<TimeEntryResponse> toResponseList(List<TimeEntry> entries);

    @Named("lawyerName")
    default String lawyerName(com.lawfirm.domain.model.Lawyer lawyer) {
        return lawyer != null ? lawyer.getFirstName() + " " + lawyer.getLastName() : null;
    }

    @Named("calcAmount")
    default BigDecimal calcAmount(TimeEntry entry) {
        if (entry.getHours() == null || entry.getHourlyRate() == null) return BigDecimal.ZERO;
        return entry.getHours().multiply(entry.getHourlyRate());
    }
}
