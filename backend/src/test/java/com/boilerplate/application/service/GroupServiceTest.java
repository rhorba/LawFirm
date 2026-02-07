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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyList;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupMapper groupMapper;

    @InjectMocks
    private GroupService groupService;

    @Test
    void testCreateGroup_Success() {
        // Arrange
        GroupRequest request = new GroupRequest("Engineering", "Engineering team", Set.of(1L));
        Group group = Group.builder().name("Engineering").description("Engineering team").build();
        GroupResponse response = new GroupResponse(
            1L, "Engineering", "Engineering team", Set.of(), Set.of(), 0, null, null
        );
        Role role = Role.builder().id(1L).name("DEVELOPER").build();

        when(groupRepository.existsByName("Engineering")).thenReturn(false);
        when(groupMapper.toEntity(request)).thenReturn(group);
        when(roleRepository.findAllById(Set.of(1L))).thenReturn(List.of(role));
        when(groupRepository.save(any(Group.class))).thenReturn(group);
        when(groupMapper.toResponse(group)).thenReturn(response);

        // Act
        GroupResponse result = groupService.createGroup(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Engineering");
        verify(groupRepository).save(any(Group.class));
    }

    @Test
    void testCreateGroup_DuplicateName_ThrowsException() {
        // Arrange
        GroupRequest request = new GroupRequest("Engineering", "Engineering team", Set.of());
        when(groupRepository.existsByName("Engineering")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> groupService.createGroup(request))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("Group already exists with name: Engineering");
    }

    @Test
    void testUpdateGroup_Success() {
        // Arrange
        Long groupId = 1L;
        GroupRequest request = new GroupRequest("Updated Name", "Updated description", Set.of(1L));
        Group group = Group.builder()
            .id(groupId)
            .name("Old Name")
            .description("Old desc")
            .roles(new HashSet<>())
            .build();
        GroupResponse response = new GroupResponse(
            groupId, "Updated Name", "Updated description", Set.of(), Set.of(), 0, null, null
        );
        Role role = Role.builder().id(1L).name("DEVELOPER").build();

        when(groupRepository.findByIdWithRoles(groupId)).thenReturn(Optional.of(group));
        when(groupRepository.existsByName("Updated Name")).thenReturn(false);
        when(roleRepository.findAllById(Set.of(1L))).thenReturn(List.of(role));
        when(groupRepository.save(any(Group.class))).thenReturn(group);
        when(groupMapper.toResponse(group)).thenReturn(response);

        // Act
        GroupResponse result = groupService.updateGroup(groupId, request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Updated Name");
        verify(groupRepository).save(any(Group.class));
    }

    @Test
    void testDeleteGroup_WithUsers_ThrowsException() {
        // Arrange
        Long groupId = 1L;
        User user = User.builder().id(1L).username("testuser").build();
        Group group = Group.builder()
            .id(groupId)
            .name("Engineering")
            .users(new HashSet<>(Set.of(user)))
            .build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        // Act & Assert
        assertThatThrownBy(() -> groupService.deleteGroup(groupId))
            .isInstanceOf(GroupHasUsersException.class)
            .hasMessageContaining("Cannot delete group with existing users");
    }

    @Test
    void testAssignUsers_ValidUserIds_Success() {
        // Arrange
        Long groupId = 1L;
        GroupAssignUsersRequest request = new GroupAssignUsersRequest(Set.of(1L, 2L));
        Group group = Group.builder()
            .id(groupId)
            .name("Engineering")
            .users(new HashSet<>())
            .roles(new HashSet<>())
            .build();
        User user1 = User.builder().id(1L).username("user1").groups(new HashSet<>()).build();
        User user2 = User.builder().id(2L).username("user2").groups(new HashSet<>()).build();
        GroupResponse response = new GroupResponse(
            groupId, "Engineering", "Eng team", Set.of(), Set.of(), 2, null, null
        );

        when(groupRepository.findByIdWithRolesAndUsers(groupId)).thenReturn(Optional.of(group));
        when(userRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(user1, user2));
        when(userRepository.saveAll(anyList())).thenReturn(List.of(user1, user2));
        when(groupRepository.save(any(Group.class))).thenReturn(group);
        when(groupMapper.toResponse(group)).thenReturn(response);

        // Act
        GroupResponse result = groupService.assignUsersToGroup(groupId, request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.userCount()).isEqualTo(2);
        verify(userRepository).saveAll(anyList());
        verify(groupRepository).save(any(Group.class));
    }

    @Test
    void testAssignUsers_InvalidUserId_ThrowsException() {
        // Arrange
        Long groupId = 1L;
        GroupAssignUsersRequest request = new GroupAssignUsersRequest(Set.of(1L, 999L));
        Group group = Group.builder().id(groupId).name("Engineering").build();
        User user1 = User.builder().id(1L).username("user1").build();

        when(groupRepository.findByIdWithRolesAndUsers(groupId)).thenReturn(Optional.of(group));
        when(userRepository.findAllById(Set.of(1L, 999L))).thenReturn(List.of(user1));

        // Act & Assert
        assertThatThrownBy(() -> groupService.assignUsersToGroup(groupId, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("One or more user IDs not found");
    }

    @Test
    void testGetGroupById_NotFound_ThrowsException() {
        // Arrange
        Long groupId = 999L;
        when(groupRepository.findByIdWithRolesAndUsers(groupId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> groupService.getGroupById(groupId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Group not found with id: 999");
    }
}
