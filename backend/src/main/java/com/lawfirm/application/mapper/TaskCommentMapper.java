package com.lawfirm.application.mapper;

import com.lawfirm.application.dto.response.TaskCommentResponse;
import com.lawfirm.domain.model.TaskComment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TaskCommentMapper {

    @Mapping(target = "taskId",        source = "task.id")
    @Mapping(target = "authorUserId",  source = "author.id")
    @Mapping(target = "authorUsername", source = "author.username")
    TaskCommentResponse toResponse(TaskComment comment);

    List<TaskCommentResponse> toResponseList(List<TaskComment> comments);
}
