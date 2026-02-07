package com.boilerplate.application.mapper;

import com.boilerplate.application.dto.request.GroupRequest;
import com.boilerplate.application.dto.response.GroupResponse;
import com.boilerplate.application.dto.response.UserSummary;
import com.boilerplate.domain.model.Group;
import com.boilerplate.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {RoleMapper.class})
public interface GroupMapper {

    @Mapping(target = "users", expression = "java(mapUsers(group.getUsers()))")
    @Mapping(target = "userCount", expression = "java(group.getUsers().size())")
    GroupResponse toResponse(Group group);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "users", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Group toEntity(GroupRequest request);

    default Set<UserSummary> mapUsers(Set<User> users) {
        if (users == null) {
            return Set.of();
        }
        return users.stream()
            .map(user -> new UserSummary(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getEnabled()
            ))
            .collect(Collectors.toSet());
    }
}
