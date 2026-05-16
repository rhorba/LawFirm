package com.lawfirm.application.service;

import com.lawfirm.application.dto.request.ChangeStatusRequest;
import com.lawfirm.application.dto.request.CreateCaseRequest;
import com.lawfirm.application.dto.response.CaseResponse;
import com.lawfirm.application.mapper.CaseMapper;
import com.lawfirm.domain.model.Case;
import com.lawfirm.domain.model.CasePriority;
import com.lawfirm.domain.model.CaseStatus;
import com.lawfirm.domain.model.CaseType;
import com.lawfirm.domain.model.Lawyer;
import com.lawfirm.domain.model.Tribunal;
import com.lawfirm.domain.repository.CaseCategoryRepository;
import com.lawfirm.domain.repository.CaseRepository;
import com.lawfirm.domain.repository.CaseStatusRepository;
import com.lawfirm.domain.repository.CaseTypeRepository;
import com.lawfirm.domain.repository.ClientRepository;
import com.lawfirm.domain.repository.LawyerRepository;
import com.lawfirm.domain.repository.TribunalRepository;
import com.lawfirm.infrastructure.security.UserPrincipal;
import com.lawfirm.presentation.exception.BusinessRuleException;
import com.lawfirm.presentation.exception.InvalidStatusTransitionException;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaseServiceTest {

    @Mock private CaseRepository caseRepository;
    @Mock private TribunalRepository tribunalRepository;
    @Mock private CaseTypeRepository caseTypeRepository;
    @Mock private CaseCategoryRepository caseCategoryRepository;
    @Mock private LawyerRepository lawyerRepository;
    @Mock private CaseStatusRepository caseStatusRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private CaseSequenceService caseSequenceService;
    @Mock private CaseNumberGenerator caseNumberGenerator;
    @Mock private CaseMapper caseMapper;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private CaseService caseService;

    private Case testCase;
    private CaseResponse testCaseResponse;
    private CaseType testCaseType;
    private CaseStatus testStatus;
    private Tribunal testTribunal;
    private Lawyer testLawyer;
    private UserPrincipal currentUser;

    @BeforeEach
    void setUp() {
        testStatus = new CaseStatus();
        testStatus.setId(1L);
        testStatus.setCode("DRAFT");
        testStatus.setIsTerminal(false);

        testCaseType = new CaseType();
        testCaseType.setId(1L);
        testCaseType.setCode("PEN");
        testCaseType.setNumberFormatTemplate("{TYPE}-{YEAR}/{TRIBUNAL}/{SEQ}");
        testCaseType.setAllowedStatuses(Set.of(testStatus));

        testTribunal = new Tribunal();
        testTribunal.setId(1L);
        testTribunal.setCode("TRB-001");

        testLawyer = new Lawyer();
        testLawyer.setId(1L);

        testCase = Case.builder()
            .id(1L)
            .fullCaseNumber("PEN-2026/TRB-001/001")
            .caseType(testCaseType)
            .status(testStatus)
            .priority(CasePriority.NORMAL)
            .build();

        testCaseResponse = mock(CaseResponse.class);
        lenient().when(testCaseResponse.id()).thenReturn(1L);
        lenient().when(testCaseResponse.fullCaseNumber()).thenReturn("PEN-2026/TRB-001/001");

        SecurityContext ctx = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(ctx.getAuthentication()).thenReturn(auth);
        lenient().when(auth.getName()).thenReturn("admin");
        SecurityContextHolder.setContext(ctx);

        currentUser = mock(UserPrincipal.class);
        lenient().when(currentUser.getUsername()).thenReturn("admin");
    }

    @Test
    void findById_ShouldReturnCase_WhenExists() {
        when(caseRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(testCase));
        when(caseMapper.toResponse(testCase)).thenReturn(testCaseResponse);

        CaseResponse result = caseService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.fullCaseNumber()).isEqualTo("PEN-2026/TRB-001/001");
    }

    @Test
    void findById_ShouldThrow_WhenNotFound() {
        when(caseRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> caseService.findById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Case not found");
    }

    @Test
    void createCase_ShouldSave_WhenAllReferencesValid() {
        CreateCaseRequest request = mock(CreateCaseRequest.class);
        when(request.caseTypeCode()).thenReturn("PEN");
        when(request.tribunalCode()).thenReturn("TRB-001");
        when(request.lawyerIds()).thenReturn(Set.of(1L));
        when(request.caseCategoryCode()).thenReturn(null);
        when(request.initialStatusCode()).thenReturn(null);
        when(request.priority()).thenReturn(CasePriority.NORMAL);
        when(request.registrationDate()).thenReturn(LocalDate.now());
        when(request.clientId()).thenReturn(null);
        when(request.parentCaseId()).thenReturn(null);

        when(caseTypeRepository.findByCodeAndActiveTrue("PEN")).thenReturn(Optional.of(testCaseType));
        when(tribunalRepository.findByCodeAndActiveTrue("TRB-001")).thenReturn(Optional.of(testTribunal));
        when(lawyerRepository.findAllById(Set.of(1L))).thenReturn(List.of(testLawyer));
        when(caseStatusRepository.findByCode("DRAFT")).thenReturn(Optional.of(testStatus));
        when(caseSequenceService.getNextSequence(anyInt(), eq("PEN"))).thenReturn(1);
        when(caseNumberGenerator.generate(any(), anyInt(), any(), any(), anyInt()))
            .thenReturn("PEN-2026/TRB-001/001");
        when(caseRepository.save(any(Case.class))).thenReturn(testCase);
        when(caseMapper.toResponse(testCase)).thenReturn(testCaseResponse);

        CaseResponse result = caseService.createCase(request, currentUser);

        assertThat(result).isNotNull();
        verify(caseRepository).save(any(Case.class));
    }

    @Test
    void createCase_ShouldThrow_WhenCaseTypeNotFound() {
        CreateCaseRequest request = mock(CreateCaseRequest.class);
        when(request.caseTypeCode()).thenReturn("UNKNOWN");
        when(caseTypeRepository.findByCodeAndActiveTrue("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> caseService.createCase(request, currentUser))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("CaseType not found");
    }

    @Test
    void createCase_ShouldThrow_WhenLawyerNotFound() {
        CreateCaseRequest request = mock(CreateCaseRequest.class);
        when(request.caseTypeCode()).thenReturn("PEN");
        when(request.tribunalCode()).thenReturn("TRB-001");
        when(request.lawyerIds()).thenReturn(Set.of(1L, 2L));
        when(caseTypeRepository.findByCodeAndActiveTrue("PEN")).thenReturn(Optional.of(testCaseType));
        when(tribunalRepository.findByCodeAndActiveTrue("TRB-001")).thenReturn(Optional.of(testTribunal));
        when(lawyerRepository.findAllById(anyCollection())).thenReturn(List.of(testLawyer)); // only 1 of 2

        assertThatThrownBy(() -> caseService.createCase(request, currentUser))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("lawyers not found");
    }

    @Test
    void deleteCase_ShouldSetDeletedAt_WhenNotTerminal() {
        when(caseRepository.findById(1L)).thenReturn(Optional.of(testCase));
        when(caseRepository.save(testCase)).thenReturn(testCase);

        caseService.deleteCase(1L);

        assertThat(testCase.getDeletedAt()).isNotNull();
    }

    @Test
    void deleteCase_ShouldThrow_WhenStatusIsTerminal() {
        testStatus.setIsTerminal(true);
        when(caseRepository.findById(1L)).thenReturn(Optional.of(testCase));

        assertThatThrownBy(() -> caseService.deleteCase(1L))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("cannot be deleted");
    }

    @Test
    void changeStatus_ShouldThrow_WhenCaseIsArchived() {
        testStatus.setCode("ARCHIVED");
        when(caseRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(testCase));
        ChangeStatusRequest request = mock(ChangeStatusRequest.class);
        when(request.statusCode()).thenReturn("ACTIVE");

        assertThatThrownBy(() -> caseService.changeStatus(1L, request, currentUser))
            .isInstanceOf(InvalidStatusTransitionException.class)
            .hasMessageContaining("ARCHIVED");
    }

    @Test
    void changeStatus_ShouldThrow_WhenClosedButNotArchiving() {
        testStatus.setCode("CLOSED");
        when(caseRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(testCase));
        ChangeStatusRequest request = mock(ChangeStatusRequest.class);
        when(request.statusCode()).thenReturn("ACTIVE");

        assertThatThrownBy(() -> caseService.changeStatus(1L, request, currentUser))
            .isInstanceOf(InvalidStatusTransitionException.class)
            .hasMessageContaining("only be archived");
    }
}
