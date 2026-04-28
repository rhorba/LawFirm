package com.lawfirm.application.service;

import com.lawfirm.application.dto.request.GroupAssignUsersRequest;
import com.lawfirm.application.dto.request.GroupRequest;
import com.lawfirm.application.dto.response.GroupResponse;
import com.lawfirm.application.mapper.GroupMapper;
import com.lawfirm.domain.model.Group;
import com.lawfirm.domain.model.Permission;
import com.lawfirm.domain.model.Role;
import com.lawfirm.domain.model.User;
import com.lawfirm.domain.repository.GroupRepository;
import com.lawfirm.domain.repository.PermissionRepository;
import com.lawfirm.domain.repository.RoleRepository;
import com.lawfirm.domain.repository.UserRepository;
import com.lawfirm.presentation.exception.DuplicateResourceException;
import com.lawfirm.presentation.exception.GroupHasUsersException;
import com.lawfirm.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final GroupMapper groupMapper;

    @Transactional(readOnly = true)
    public List<GroupResponse> getAllGroups() {
        return groupRepository.findAllWithRolesAndUsers().stream()
            .map(groupMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroupById(Long id) {
        Group group = groupRepository.findByIdWithRolesAndUsers(id)
            .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + id));
        return groupMapper.toResponse(group);
    }

    @Transactional
    public GroupResponse createGroup(GroupRequest request) {
        if (groupRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Group already exists with name: " + request.name());
        }

        Group group = groupMapper.toEntity(request);

        if (request.roleIds() != null && !request.roleIds().isEmpty()) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(request.roleIds()));
            if (roles.size() != request.roleIds().size()) {
                throw new ResourceNotFoundException("One or more role IDs not found");
            }
            group.setRoles(roles);
        }

        if (request.permissionIds() != null && !request.permissionIds().isEmpty()) {
            Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(request.permissionIds()));
            if (permissions.size() != request.permissionIds().size()) {
                throw new ResourceNotFoundException("One or more permission IDs not found");
            }
            group.setPermissions(permissions);
        }

        Group savedGroup = groupRepository.save(group);
        return groupMapper.toResponse(savedGroup);
    }

    @Transactional
    public GroupResponse updateGroup(Long id, GroupRequest request) {
        Group group = groupRepository.findByIdWithRoles(id)
            .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + id));

        if (!group.getName().equals(request.name()) && groupRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Group already exists with name: " + request.name());
        }

        group.setName(request.name());
        group.setDescription(request.description());

        if (request.roleIds() != null) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(request.roleIds()));
            if (roles.size() != request.roleIds().size()) {
                throw new ResourceNotFoundException("One or more role IDs not found");
            }
            group.setRoles(roles);
        } else {
            group.getRoles().clear();
        }

        if (request.permissionIds() != null) {
            Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(request.permissionIds()));
            if (permissions.size() != request.permissionIds().size()) {
                throw new ResourceNotFoundException("One or more permission IDs not found");
            }
            group.setPermissions(permissions);
        } else {
            group.getPermissions().clear();
        }

        Group updatedGroup = groupRepository.save(group);
        return groupMapper.toResponse(updatedGroup);
    }

    @Transactional
    public void deleteGroup(Long id) {
        Group group = groupRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + id));

        if (!group.getUsers().isEmpty()) {
            throw new GroupHasUsersException(
                "Cannot delete group with existing users. Please remove all users first."
            );
        }

        groupRepository.delete(group);
    }

    @Transactional
    public GroupResponse assignUsersToGroup(Long groupId, GroupAssignUsersRequest request) {
        Group group = groupRepository.findByIdWithRolesAndUsers(groupId)
            .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + groupId));

        List<User> users = userRepository.findAllById(request.userIds());
        if (users.size() != request.userIds().size()) {
            throw new ResourceNotFoundException("One or more user IDs not found");
        }

        for (User user : users) {
            user.getGroups().add(group);
            group.getUsers().add(user);
        }

        userRepository.saveAll(users);
        Group updatedGroup = groupRepository.save(group);
        return groupMapper.toResponse(updatedGroup);
    }

    @Transactional
    public void removeUserFromGroup(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + groupId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.getGroups().remove(group);
        group.getUsers().remove(user);

        userRepository.save(user);
        groupRepository.save(group);
    }
}
