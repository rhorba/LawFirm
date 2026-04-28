package com.lawfirm.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.application.dto.response.AuditLogResponse;
import com.lawfirm.application.mapper.AuditLogMapper;
import com.lawfirm.domain.model.AuditLog;
import com.lawfirm.domain.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAllAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable)
            .map(auditLogMapper::toResponse);
    }

    @Transactional
    public void log(String resource, Long resourceId, String action,
                    List<String> changedFields, String username) {
        try {
            String metadata = objectMapper.writeValueAsString(
                Map.of("changedFields", changedFields)
            );
            AuditLog entry = AuditLog.builder()
                .resource(resource)
                .resourceId(resourceId.toString())
                .action(action)
                .username(username)
                .metadata(metadata)
                .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("Failed to write audit log for {} {}: {}", resource, resourceId, e.getMessage());
        }
    }

    @Transactional
    public void log(String resource, Long resourceId, String action,
                    Object oldValues, Object newValues, String username) {
        try {
            AuditLog entry = AuditLog.builder()
                .resource(resource)
                .resourceId(resourceId.toString())
                .action(action)
                .username(username)
                .oldValues(oldValues != null ? objectMapper.writeValueAsString(oldValues) : null)
                .newValues(newValues != null ? objectMapper.writeValueAsString(newValues) : null)
                .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("Failed to write snapshot audit log for {} {}: {}", resource, resourceId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> findByResource(String resource, Long resourceId) {
        return auditLogRepository
            .findByResourceAndResourceIdOrderByCreatedAtDesc(resource, resourceId.toString())
            .stream()
            .map(auditLogMapper::toResponse)
            .toList();
    }
}
