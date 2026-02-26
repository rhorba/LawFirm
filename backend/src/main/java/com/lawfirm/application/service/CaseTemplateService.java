package com.lawfirm.application.service;

import com.lawfirm.application.dto.request.CaseTemplateRequest;
import com.lawfirm.application.dto.response.CaseTemplateResponse;
import com.lawfirm.application.mapper.CaseTemplateMapper;
import com.lawfirm.domain.repository.CaseTemplateRepository;
import com.lawfirm.presentation.exception.DuplicateResourceException;
import com.lawfirm.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CaseTemplateService {

    private final CaseTemplateRepository templateRepository;
    private final CaseTemplateMapper templateMapper;

    public List<CaseTemplateResponse> findAll() {
        return templateRepository.findAll().stream()
            .map(templateMapper::toResponse)
            .toList();
    }

    @Transactional
    public CaseTemplateResponse create(CaseTemplateRequest request) {
        if (templateRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Template with name '" + request.name() + "' already exists");
        }
        return templateMapper.toResponse(
            templateRepository.save(templateMapper.toEntity(request))
        );
    }

    @Transactional
    public void delete(Long id) {
        if (!templateRepository.existsById(id)) {
            throw new ResourceNotFoundException("CaseTemplate not found with id: " + id);
        }
        templateRepository.deleteById(id);
    }
}
