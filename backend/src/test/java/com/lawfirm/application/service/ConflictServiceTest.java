package com.lawfirm.application.service;

import com.lawfirm.application.dto.request.ConflictClearRequest;
import com.lawfirm.application.dto.request.ConflictPartyRequest;
import com.lawfirm.application.dto.response.ConflictCheckResponse;
import com.lawfirm.application.dto.response.ConflictCheckResultResponse;
import com.lawfirm.application.dto.response.ConflictPartyResponse;
import com.lawfirm.application.mapper.ConflictCheckMapper;
import com.lawfirm.application.mapper.ConflictPartyMapper;
import com.lawfirm.domain.model.Case;
import com.lawfirm.domain.model.ConflictCheck;
import com.lawfirm.domain.model.ConflictCheck.CheckResult;
import com.lawfirm.domain.model.ConflictParty;
import com.lawfirm.domain.model.ConflictParty.PartyType;
import com.lawfirm.domain.model.User;
import com.lawfirm.domain.repository.CaseRepository;
import com.lawfirm.domain.repository.ClientRepository;
import com.lawfirm.domain.repository.ConflictCheckRepository;
import com.lawfirm.domain.repository.ConflictPartyRepository;
import com.lawfirm.domain.repository.UserRepository;
import com.lawfirm.presentation.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConflictServiceTest {

    @Mock private ConflictPartyRepository conflictPartyRepository;
    @Mock private ConflictCheckRepository conflictCheckRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private CaseRepository caseRepository;
    @Mock private UserRepository userRepository;
    @Mock private ConflictPartyMapper conflictPartyMapper;
    @Mock private ConflictCheckMapper conflictCheckMapper;

    @InjectMocks
    private ConflictService conflictService;

    private User testUser;
    private Case testCase;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("admin");

        testCase = new Case();
        testCase.setId(10L);

        SecurityContext ctx = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(ctx.getAuthentication()).thenReturn(auth);
        lenient().when(auth.getName()).thenReturn("admin");
        SecurityContextHolder.setContext(ctx);
    }

    @Test
    void performCheck_ShouldReturnClear_WhenNoMatches() {
        when(userRepository.findByUsernameAndDeletedAtIsNull("admin"))
            .thenReturn(Optional.of(testUser));
        when(clientRepository.searchByName("UniqueXYZ")).thenReturn(Collections.emptyList());
        when(conflictPartyRepository.searchByName("UniqueXYZ")).thenReturn(Collections.emptyList());
        when(caseRepository.searchByName("UniqueXYZ")).thenReturn(Collections.emptyList());

        ConflictCheck savedCheck = ConflictCheck.builder()
            .id(1L)
            .searchName("UniqueXYZ")
            .result(CheckResult.CLEAR)
            .build();
        when(conflictCheckRepository.save(any(ConflictCheck.class))).thenReturn(savedCheck);

        var request = mock(com.lawfirm.application.dto.request.ConflictCheckRequest.class);
        when(request.searchName()).thenReturn("UniqueXYZ");

        ConflictCheckResultResponse result = conflictService.performCheck(request);

        assertThat(result.hasConflict()).isFalse();
        assertThat(result.matches()).isEmpty();
    }

    @Test
    void performCheck_ShouldReturnConflictFound_WhenClientMatches() {
        when(userRepository.findByUsernameAndDeletedAtIsNull("admin"))
            .thenReturn(Optional.of(testUser));

        com.lawfirm.domain.model.Client client = new com.lawfirm.domain.model.Client();
        client.setId(5L);
        client.setFirstName("Ahmed");
        client.setLastName("Benali");

        when(clientRepository.searchByName("Benali")).thenReturn(List.of(client));
        when(conflictPartyRepository.searchByName("Benali")).thenReturn(Collections.emptyList());
        when(caseRepository.searchByName("Benali")).thenReturn(Collections.emptyList());

        ConflictCheck savedCheck = ConflictCheck.builder()
            .id(1L)
            .searchName("Benali")
            .result(CheckResult.CONFLICT_FOUND)
            .build();
        when(conflictCheckRepository.save(any(ConflictCheck.class))).thenReturn(savedCheck);

        var request = mock(com.lawfirm.application.dto.request.ConflictCheckRequest.class);
        when(request.searchName()).thenReturn("Benali");

        ConflictCheckResultResponse result = conflictService.performCheck(request);

        assertThat(result.hasConflict()).isTrue();
        assertThat(result.matches()).hasSize(1);
        assertThat(result.matches().get(0).source()).isEqualTo("CLIENT");
    }

    @Test
    void clearCheck_ShouldUpdateCheckWithNote() {
        ConflictCheck check = ConflictCheck.builder()
            .id(1L)
            .result(CheckResult.CONFLICT_FOUND)
            .build();
        ConflictClearRequest clearRequest = mock(ConflictClearRequest.class);
        when(clearRequest.clearedNote()).thenReturn("Verified: different person");
        when(conflictCheckRepository.findByIdWithUsers(1L)).thenReturn(Optional.of(check));
        when(userRepository.findByUsernameAndDeletedAtIsNull("admin"))
            .thenReturn(Optional.of(testUser));
        when(conflictCheckRepository.save(check)).thenReturn(check);
        ConflictCheckResponse mockCheckResponse = mock(ConflictCheckResponse.class);
        when(mockCheckResponse.id()).thenReturn(1L);
        when(conflictCheckMapper.toResponse(check)).thenReturn(mockCheckResponse);

        ConflictCheckResponse result = conflictService.clearCheck(1L, clearRequest);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(check.getClearedNote()).isEqualTo("Verified: different person");
    }

    @Test
    void clearCheck_ShouldThrow_WhenCheckNotFound() {
        ConflictClearRequest clearRequest = mock(ConflictClearRequest.class);
        when(conflictCheckRepository.findByIdWithUsers(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conflictService.clearCheck(99L, clearRequest))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addParty_ShouldSaveParty() {
        ConflictPartyRequest request = mock(ConflictPartyRequest.class);
        when(request.partyType()).thenReturn(PartyType.OPPOSING_PARTY);
        when(request.name()).thenReturn("Opposing Corp");
        when(caseRepository.findById(10L)).thenReturn(Optional.of(testCase));
        ConflictParty saved = ConflictParty.builder().id(1L).name("Opposing Corp").build();
        when(conflictPartyRepository.save(any(ConflictParty.class))).thenReturn(saved);
        ConflictPartyResponse mockPartyResponse = mock(ConflictPartyResponse.class);
        when(mockPartyResponse.name()).thenReturn("Opposing Corp");
        when(conflictPartyMapper.toResponse(saved)).thenReturn(mockPartyResponse);

        ConflictPartyResponse result = conflictService.addParty(10L, request);

        assertThat(result.name()).isEqualTo("Opposing Corp");
    }

    @Test
    void addParty_ShouldThrow_WhenCaseNotFound() {
        ConflictPartyRequest request = mock(ConflictPartyRequest.class);
        when(caseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conflictService.addParty(99L, request))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteParty_ShouldCallRepository_WhenExists() {
        when(conflictPartyRepository.existsById(1L)).thenReturn(true);

        conflictService.deleteParty(1L);

        verify(conflictPartyRepository).deleteById(1L);
    }

    @Test
    void deleteParty_ShouldThrow_WhenNotFound() {
        when(conflictPartyRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> conflictService.deleteParty(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
