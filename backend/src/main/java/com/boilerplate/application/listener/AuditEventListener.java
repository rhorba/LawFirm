package com.boilerplate.application.listener;

import com.boilerplate.application.event.AuditEvent;
import com.boilerplate.domain.model.AuditLog;
import com.boilerplate.domain.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventListener {

    private final AuditLogRepository auditLogRepository;

    @EventListener
    @Async
    public void handleAuditEvent(AuditEvent event) {
        log.debug("Handling audit event: {}", event);
        try {
            AuditLog auditLog = AuditLog.builder()
                .userId(event.userId())
                .username(event.username())
                .action(event.action())
                .resource(event.resource())
                .resourceId(event.resourceId())
                .metadata(event.metadata())
                .ipAddress(event.ipAddress())
                .build();
            
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }
    }
}
