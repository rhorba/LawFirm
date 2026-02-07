package com.boilerplate.application.mapper;

import com.boilerplate.application.dto.response.PermissionResponse;
import com.boilerplate.domain.model.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PermissionMapper {

    @Mapping(target = "resource", expression = "java(permission.getResource().name())")
    @Mapping(target = "action", expression = "java(permission.getAction().name())")
    PermissionResponse toResponse(Permission permission);
}
