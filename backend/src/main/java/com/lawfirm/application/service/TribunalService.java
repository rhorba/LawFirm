package com.lawfirm.application.service;

import com.lawfirm.application.dto.response.TribunalResponse;
import com.lawfirm.application.mapper.TribunalMapper;
import com.lawfirm.domain.repository.TribunalRepository;
import com.lawfirm.presentation.exception.ResourceNotFoundException;
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
