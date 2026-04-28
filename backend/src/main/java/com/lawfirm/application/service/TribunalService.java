package com.lawfirm.application.service;

import com.lawfirm.application.dto.response.TribunalResponse;
import com.lawfirm.application.mapper.TribunalMapper;
import com.lawfirm.domain.model.Tribunal;
import com.lawfirm.domain.repository.TribunalRepository;
import com.lawfirm.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

    public Page<TribunalResponse> search(String query, Pageable pageable) {
        Specification<Tribunal> spec = (root, q, cb) -> {
            if (query == null || query.isBlank()) {
                return cb.equal(root.get("active"), true);
            }
            String pattern = "%" + query.toLowerCase() + "%";
            return cb.and(
                cb.equal(root.get("active"), true),
                cb.or(
                    cb.like(cb.lower(root.get("nameFr")), pattern),
                    cb.like(cb.lower(root.get("nameAr")), pattern),
                    cb.like(cb.lower(root.get("code")),   pattern)
                )
            );
        };
        return tribunalRepository.findAll(spec, pageable).map(tribunalMapper::toResponse);
    }

    public TribunalResponse findByCode(String code) {
        return tribunalRepository.findByCodeAndActiveTrue(code)
            .map(tribunalMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Tribunal not found with code: " + code));
    }
}
