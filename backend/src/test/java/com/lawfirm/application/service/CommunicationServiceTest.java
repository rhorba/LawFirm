package com.lawfirm.application.service;

import com.lawfirm.application.dto.request.CommunicationRequest;
import com.lawfirm.application.dto.request.EmailSendRequest;
import com.lawfirm.application.dto.response.CommunicationResponse;
import com.lawfirm.application.mapper.CommunicationMapper;
import com.lawfirm.domain.model.Case;
import com.lawfirm.domain.model.Communication;
import com.lawfirm.domain.model.Communication.CommunicationType;
import com.lawfirm.domain.model.Communication.CommunicationDirection;
import com.lawfirm.domain.model.User;
import com.lawfirm.domain.repository.CaseRepository;
import com.lawfirm.domain.repository.CommunicationRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunicationServiceTest {

    @Mock private CommunicationRepository communicationRepository;
    @Mock private CaseRepository caseRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailSenderService emailSenderService;
    @Mock private CommunicationMapper communicationMapper;

    @InjectMocks
    private CommunicationService communicationService;

    private Communication testComm;
    private CommunicationResponse testResponse;
    private Case testCase;
    private User testUser;

    @BeforeEach
    void setUp() {
        testCase = new Case();
        testCase.setId(10L);

        testUser = new User();
        testUser.setUsername("admin");

        testComm = Communication.builder()
            .id(1L)
            .caseEntity(testCase)
            .commType(CommunicationType.NOTE)
            .direction(CommunicationDirection.INTERNAL)
            .subject("Case update")
            .sentBy(testUser)
            .build();

        testResponse = mock(CommunicationResponse.class);
        lenient().when(testResponse.id()).thenReturn(1L);
        lenient().when(testResponse.subject()).thenReturn("Case update");
    }

    private void mockSecurityContext() {
        SecurityContext ctx = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(ctx.getAuthentication()).thenReturn(auth);
        lenient().when(auth.getName()).thenReturn("admin");
        SecurityContextHolder.setContext(ctx);
        lenient().when(userRepository.findByUsernameAndDeletedAtIsNull("admin"))
            .thenReturn(Optional.of(testUser));
    }

    @Test
    void findByCaseId_ShouldReturnList() {
        when(communicationRepository.findByCaseId(10L)).thenReturn(List.of(testComm));
        when(communicationMapper.toResponse(testComm)).thenReturn(testResponse);

        List<CommunicationResponse> result = communicationService.findByCaseId(10L);

        assertThat(result).hasSize(1);
    }

    @Test
    void log_ShouldSaveCommunication() {
        mockSecurityContext();
        CommunicationRequest request = mock(CommunicationRequest.class);
        when(request.commType()).thenReturn(CommunicationType.NOTE);
        when(request.direction()).thenReturn(CommunicationDirection.INTERNAL);
        when(request.subject()).thenReturn("Update");
        when(caseRepository.findById(10L)).thenReturn(Optional.of(testCase));
        when(communicationRepository.save(any(Communication.class))).thenReturn(testComm);
        when(communicationMapper.toResponse(testComm)).thenReturn(testResponse);

        CommunicationResponse result = communicationService.log(10L, request);

        assertThat(result).isNotNull();
        verify(communicationRepository).save(any(Communication.class));
    }

    @Test
    void log_ShouldThrow_WhenCaseNotFound() {
        mockSecurityContext();
        CommunicationRequest request = mock(CommunicationRequest.class);
        when(caseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> communicationService.log(99L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Case not found");
    }

    @Test
    void sendEmail_ShouldCallEmailSenderAndSaveRecord() {
        mockSecurityContext();
        EmailSendRequest request = mock(EmailSendRequest.class);
        when(request.to()).thenReturn("client@example.com");
        when(request.subject()).thenReturn("Hearing notice");
        when(request.body()).thenReturn("Please attend on Monday.");
        when(caseRepository.findById(10L)).thenReturn(Optional.of(testCase));
        when(communicationRepository.save(any(Communication.class))).thenReturn(testComm);
        when(communicationMapper.toResponse(testComm)).thenReturn(testResponse);

        communicationService.sendEmail(10L, request);

        verify(emailSenderService).send("client@example.com", "Hearing notice", "Please attend on Monday.");
        verify(communicationRepository).save(any(Communication.class));
    }

    @Test
    void sendEmail_ShouldThrow_WhenCaseNotFound() {
        mockSecurityContext();
        EmailSendRequest request = mock(EmailSendRequest.class);
        when(caseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> communicationService.sendEmail(99L, request))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_ShouldCallRepository_WhenCommunicationExists() {
        when(communicationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(testComm));

        communicationService.delete(1L);

        verify(communicationRepository).delete(testComm);
    }

    @Test
    void delete_ShouldThrow_WhenNotFound() {
        when(communicationRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> communicationService.delete(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Communication not found");
    }
}
