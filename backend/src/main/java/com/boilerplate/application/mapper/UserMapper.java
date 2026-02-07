package com.boilerplate.application.mapper;

import com.boilerplate.application.dto.request.CreateUserRequest;
import com.boilerplate.application.dto.request.UpdateUserRequest;
import com.boilerplate.application.dto.response.RoleResponse;
import com.boilerplate.application.dto.response.UserResponse;
import com.boilerplate.domain.model.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {RoleMapper.class, GroupMapper.class})
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)  // Handled separately with encoding
    @Mapping(target = "groups", ignore = true)  // Handled separately
    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "accountNonExpired", constant = "true")
    @Mapping(target = "accountNonLocked", constant = "true")
    @Mapping(target = "credentialsNonExpired", constant = "true")
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    User toEntity(CreateUserRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)  // Only update if provided
    @Mapping(target = "groups", ignore = true)  // Handled separately
    @Mapping(target = "accountNonExpired", ignore = true)
    @Mapping(target = "accountNonLocked", ignore = true)
    @Mapping(target = "credentialsNonExpired", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget User user, UpdateUserRequest request);

    @Mapping(target = "roles", expression = "java(extractRolesFromGroups(user))")
    @Mapping(target = "groups", source = "groups")
    UserResponse toResponse(User user);

    default Set<RoleResponse> extractRolesFromGroups(User user) {
        return user.getGroups().stream()
            .flatMap(group -> group.getRoles().stream())
            .distinct()
            .map(role -> {
                Set<com.boilerplate.application.dto.response.PermissionResponse> permissions =
                    role.getPermissions().stream()
                        .map(permission -> new com.boilerplate.application.dto.response.PermissionResponse(
                            permission.getId(),
                            permission.getName(),
                            permission.getDescription(),
                            permission.getResource().name(),
                            permission.getAction().name()
                        ))
                        .collect(Collectors.toSet());

                return new RoleResponse(
                    role.getId(),
                    role.getName(),
                    role.getDescription(),
                    permissions
                );
            })
            .collect(Collectors.toSet());
    }
}
