package com.boilerplate.application.mapper;

import com.boilerplate.application.dto.response.RoleResponse;
import com.boilerplate.domain.model.Role;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {PermissionMapper.class})
public interface RoleMapper {

    RoleResponse toResponse(Role role);
}
