package com.lawfirm.application.mapper;

import com.lawfirm.application.dto.response.RoleResponse;
import com.lawfirm.domain.model.Role;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {PermissionMapper.class})
public interface RoleMapper {

    RoleResponse toResponse(Role role);
}
