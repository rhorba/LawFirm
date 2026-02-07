package com.boilerplate.application.service;

import com.boilerplate.application.dto.response.AuditLogResponse;
import com.boilerplate.application.mapper.AuditLogMapper;
import com.boilerplate.domain.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAllAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable)
            .map(auditLogMapper::toResponse);
    }
}
