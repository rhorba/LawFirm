package com.boilerplate.application.service;

import com.boilerplate.application.dto.response.TribunalResponse;
import com.boilerplate.application.mapper.TribunalMapper;
import com.boilerplate.domain.repository.TribunalRepository;
import com.boilerplate.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TribunalService {

    private final TribunalRepository tribunalRepository;
    private final TribunalMapper tribunalMapper;

    public List<TribunalResponse> findAll() {
        return tribunalMapper.toResponseList(tribunalRepository.findAllByActiveTrue());
    }

    public TribunalResponse findByCode(String code) {
        return tribunalRepository.findByCodeAndActiveTrue(code)
            .map(tribunalMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Tribunal not found with code: " + code));
    }
}
