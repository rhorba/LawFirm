package com.boilerplate.application.service;

import com.boilerplate.application.dto.request.GroupAssignUsersRequest;
import com.boilerplate.application.dto.request.GroupRequest;
import com.boilerplate.application.dto.response.GroupResponse;
import com.boilerplate.application.mapper.GroupMapper;
import com.boilerplate.domain.model.Group;
import com.boilerplate.domain.model.Role;
import com.boilerplate.domain.model.User;
import com.boilerplate.domain.repository.GroupRepository;
import com.boilerplate.domain.repository.RoleRepository;
import com.boilerplate.domain.repository.UserRepository;
import com.boilerplate.presentation.exception.DuplicateResourceException;
import com.boilerplate.presentation.exception.GroupHasUsersException;
import com.boilerplate.presentation.exception.ResourceNotFoundException;
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
        // Check for duplicate name
        if (groupRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Group already exists with name: " + request.name());
        }

        Group group = groupMapper.toEntity(request);

        // Assign roles if provided
        if (request.roleIds() != null && !request.roleIds().isEmpty()) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(request.roleIds()));
            if (roles.size() != request.roleIds().size()) {
                throw new ResourceNotFoundException("One or more role IDs not found");
            }
            group.setRoles(roles);
        }

        Group savedGroup = groupRepository.save(group);
        return groupMapper.toResponse(savedGroup);
    }

    @Transactional
    public GroupResponse updateGroup(Long id, GroupRequest request) {
        Group group = groupRepository.findByIdWithRoles(id)
            .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + id));

        // Check for duplicate name (excluding current group)
        if (!group.getName().equals(request.name()) && groupRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Group already exists with name: " + request.name());
        }

        group.setName(request.name());
        group.setDescription(request.description());

        // Update roles
        if (request.roleIds() != null) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(request.roleIds()));
            if (roles.size() != request.roleIds().size()) {
                throw new ResourceNotFoundException("One or more role IDs not found");
            }
            group.setRoles(roles);
        } else {
            group.getRoles().clear();
        }

        Group updatedGroup = groupRepository.save(group);
        return groupMapper.toResponse(updatedGroup);
    }

    @Transactional
    public void deleteGroup(Long id) {
        Group group = groupRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + id));

        // Prevent deletion if group has users
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

        // Add users to group
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
