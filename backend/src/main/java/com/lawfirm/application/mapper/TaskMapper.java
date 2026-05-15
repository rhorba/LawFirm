package com.lawfirm.application.mapper;

import com.lawfirm.application.dto.response.TaskResponse;
import com.lawfirm.application.dto.response.UpcomingTaskResponse;
import com.lawfirm.domain.model.Lawyer;
import com.lawfirm.domain.model.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(target = "caseId",             source = "caseEntity.id")
    @Mapping(target = "caseNumber",         source = "caseEntity.fullCaseNumber")
    @Mapping(target = "assignedLawyerId",   source = "assignedLawyer.id")
    @Mapping(target = "assignedLawyerName", source = "assignedLawyer", qualifiedByName = "lawyerFullName")
    @Mapping(target = "createdByUserId",    source = "createdBy.id")
    @Mapping(target = "createdByUsername",  source = "createdBy.username")
    TaskResponse toResponse(Task task);

    @Mapping(target = "caseId",             source = "caseEntity.id")
    @Mapping(target = "caseNumber",         source = "caseEntity.fullCaseNumber")
    @Mapping(target = "assignedLawyerId",   source = "assignedLawyer.id")
    @Mapping(target = "assignedLawyerName", source = "assignedLawyer", qualifiedByName = "lawyerFullName")
    UpcomingTaskResponse toUpcomingResponse(Task task);

    List<TaskResponse> toResponseList(List<Task> tasks);

    List<UpcomingTaskResponse> toUpcomingList(List<Task> tasks);

    @Named("lawyerFullName")
    default String lawyerFullName(Lawyer lawyer) {
        return lawyer != null ? lawyer.getFirstName() + " " + lawyer.getLastName() : null;
    }
}
