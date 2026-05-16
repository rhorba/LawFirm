package com.lawfirm.application.service;

import com.lawfirm.application.dto.request.CreateLawyerRequest;
import com.lawfirm.application.dto.request.UpdateLawyerRequest;
import com.lawfirm.application.dto.response.LawyerResponse;
import com.lawfirm.application.mapper.LawyerMapper;
import com.lawfirm.domain.model.Lawyer;
import com.lawfirm.domain.repository.LawyerRepository;
import com.lawfirm.presentation.exception.DuplicateResourceException;
import com.lawfirm.presentation.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LawyerServiceTest {

    @Mock
    private LawyerRepository lawyerRepository;

    @Mock
    private LawyerMapper lawyerMapper;

    @InjectMocks
    private LawyerService lawyerService;

    private Lawyer testLawyer;
    private LawyerResponse testLawyerResponse;

    @BeforeEach
    void setUp() {
        testLawyer = new Lawyer();
        testLawyer.setId(1L);
        testLawyer.setFirstName("Karim");
        testLawyer.setLastName("Alaoui");
        testLawyer.setTaxId("TAX-001");
        testLawyer.setActive(true);

        testLawyerResponse = mock(LawyerResponse.class);
        lenient().when(testLawyerResponse.id()).thenReturn(1L);
        lenient().when(testLawyerResponse.firstName()).thenReturn("Karim");
        lenient().when(testLawyerResponse.lastName()).thenReturn("Alaoui");
    }

    @Test
    void findById_ShouldReturnLawyer_WhenActiveAndExists() {
        when(lawyerRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(testLawyer));
        when(lawyerMapper.toResponse(testLawyer)).thenReturn(testLawyerResponse);

        LawyerResponse result = lawyerService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.firstName()).isEqualTo("Karim");
    }

    @Test
    void findById_ShouldThrow_WhenNotFound() {
        when(lawyerRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lawyerService.findById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Lawyer not found");
    }

    @Test
    void findAll_ShouldReturnActiveLawyers() {
        when(lawyerRepository.findAllByActiveTrue()).thenReturn(List.of(testLawyer));
        when(lawyerMapper.toResponseList(anyList())).thenReturn(List.of(testLawyerResponse));

        List<LawyerResponse> result = lawyerService.findAll();

        assertThat(result).hasSize(1);
    }

    @Test
    void create_ShouldSave_WhenTaxIdIsUnique() {
        CreateLawyerRequest request = mock(CreateLawyerRequest.class);
        when(request.taxId()).thenReturn("TAX-999");
        when(lawyerRepository.findByTaxId("TAX-999")).thenReturn(Optional.empty());
        when(lawyerMapper.toEntity(request)).thenReturn(testLawyer);
        when(lawyerRepository.save(testLawyer)).thenReturn(testLawyer);
        when(lawyerMapper.toResponse(testLawyer)).thenReturn(testLawyerResponse);

        LawyerResponse result = lawyerService.create(request);

        assertThat(result).isNotNull();
        verify(lawyerRepository).save(testLawyer);
    }

    @Test
    void create_ShouldThrow_WhenDuplicateTaxId() {
        CreateLawyerRequest request = mock(CreateLawyerRequest.class);
        when(request.taxId()).thenReturn("TAX-001");
        when(lawyerRepository.findByTaxId("TAX-001")).thenReturn(Optional.of(testLawyer));

        assertThatThrownBy(() -> lawyerService.create(request))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("tax ID");

        verify(lawyerRepository, never()).save(any());
    }

    @Test
    void create_ShouldSkipTaxIdCheck_WhenTaxIdIsBlank() {
        CreateLawyerRequest request = mock(CreateLawyerRequest.class);
        when(request.taxId()).thenReturn("");
        when(lawyerMapper.toEntity(request)).thenReturn(testLawyer);
        when(lawyerRepository.save(testLawyer)).thenReturn(testLawyer);
        when(lawyerMapper.toResponse(testLawyer)).thenReturn(testLawyerResponse);

        lawyerService.create(request);

        verify(lawyerRepository, never()).findByTaxId(any());
    }

    @Test
    void update_ShouldThrow_WhenLawyerNotFound() {
        UpdateLawyerRequest request = mock(UpdateLawyerRequest.class);
        when(lawyerRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lawyerService.update(99L, request))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_ShouldThrow_WhenNewTaxIdAlreadyTaken() {
        UpdateLawyerRequest request = mock(UpdateLawyerRequest.class);
        when(request.taxId()).thenReturn("TAX-TAKEN");
        when(lawyerRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(testLawyer));
        when(lawyerRepository.findByTaxId("TAX-TAKEN")).thenReturn(Optional.of(new Lawyer()));

        assertThatThrownBy(() -> lawyerService.update(1L, request))
            .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void deactivate_ShouldSetActiveFalse() {
        when(lawyerRepository.findById(1L)).thenReturn(Optional.of(testLawyer));

        lawyerService.deactivate(1L);

        assertThat(testLawyer.getActive()).isFalse();
        verify(lawyerRepository).save(testLawyer);
    }

    @Test
    void activate_ShouldSetActiveTrue() {
        testLawyer.setActive(false);
        when(lawyerRepository.findById(1L)).thenReturn(Optional.of(testLawyer));
        when(lawyerRepository.save(testLawyer)).thenReturn(testLawyer);
        when(lawyerMapper.toResponse(testLawyer)).thenReturn(testLawyerResponse);

        LawyerResponse result = lawyerService.activate(1L);

        assertThat(testLawyer.getActive()).isTrue();
        assertThat(result).isNotNull();
    }

    @Test
    void getCaseCount_ShouldDelegateToRepository() {
        when(lawyerRepository.countActiveCases(1L)).thenReturn(5L);

        Long count = lawyerService.getCaseCount(1L);

        assertThat(count).isEqualTo(5L);
    }
}
