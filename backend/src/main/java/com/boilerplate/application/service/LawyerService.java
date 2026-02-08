package com.boilerplate.application.service;

import com.boilerplate.application.dto.request.CreateLawyerRequest;
import com.boilerplate.application.dto.request.UpdateLawyerRequest;
import com.boilerplate.application.dto.response.LawyerResponse;
import com.boilerplate.application.mapper.LawyerMapper;
import com.boilerplate.domain.model.Lawyer;
import com.boilerplate.domain.repository.LawyerRepository;
import com.boilerplate.presentation.exception.DuplicateResourceException;
import com.boilerplate.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LawyerService {

    private final LawyerRepository lawyerRepository;
    private final LawyerMapper lawyerMapper;

    public List<LawyerResponse> findAll() {
        return lawyerMapper.toResponseList(lawyerRepository.findAllByActiveTrue());
    }

    public LawyerResponse findById(Long id) {
        return lawyerRepository.findByIdAndActiveTrue(id)
            .map(lawyerMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Lawyer not found with id: " + id));
    }

    @Transactional
    public LawyerResponse create(CreateLawyerRequest request) {
        // Check tax ID uniqueness if provided
        if (request.taxId() != null && !request.taxId().isBlank()) {
            if (lawyerRepository.findByTaxId(request.taxId()).isPresent()) {
                throw new DuplicateResourceException("Lawyer with tax ID " + request.taxId() + " already exists");
            }
        }

        Lawyer lawyer = lawyerMapper.toEntity(request);
        lawyer = lawyerRepository.save(lawyer);
        return lawyerMapper.toResponse(lawyer);
    }

    @Transactional
    public LawyerResponse update(Long id, UpdateLawyerRequest request) {
        Lawyer lawyer = lawyerRepository.findByIdAndActiveTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Lawyer not found with id: " + id));

        // Check tax ID uniqueness if changed
        if (request.taxId() != null && !request.taxId().equals(lawyer.getTaxId())) {
            if (lawyerRepository.findByTaxId(request.taxId()).isPresent()) {
                throw new DuplicateResourceException("Lawyer with tax ID " + request.taxId() + " already exists");
            }
        }

        lawyerMapper.updateEntity(request, lawyer);
        lawyer = lawyerRepository.save(lawyer);
        return lawyerMapper.toResponse(lawyer);
    }

    @Transactional
    public void deactivate(Long id) {
        Lawyer lawyer = lawyerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Lawyer not found with id: " + id));
        lawyer.setActive(false);
        lawyerRepository.save(lawyer);
    }

    public Long getCaseCount(Long lawyerId) {
        return lawyerRepository.countActiveCases(lawyerId);
    }
}
