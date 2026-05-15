package com.lawfirm.application.mapper;

import com.lawfirm.application.dto.response.ConflictCheckResponse;
import com.lawfirm.domain.model.ConflictCheck;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ConflictCheckMapper {

    @Mapping(target = "performedByUsername", source = "performedBy.username")
    @Mapping(target = "clearedByUsername",   source = "clearedBy.username")
    ConflictCheckResponse toResponse(ConflictCheck check);

    List<ConflictCheckResponse> toResponseList(List<ConflictCheck> checks);
}
