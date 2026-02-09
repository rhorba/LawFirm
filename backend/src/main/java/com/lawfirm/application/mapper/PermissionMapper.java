package com.lawfirm.application.mapper;

import com.lawfirm.application.dto.response.PermissionResponse;
import com.lawfirm.domain.model.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PermissionMapper {

    @Mapping(target = "resource", expression = "java(permission.getResource().name())")
    @Mapping(target = "action", expression = "java(permission.getAction().name())")
    PermissionResponse toResponse(Permission permission);
}
