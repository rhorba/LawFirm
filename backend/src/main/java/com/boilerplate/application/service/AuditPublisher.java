package com.boilerplate.application.service;

import com.boilerplate.application.event.AuditEvent;
import com.boilerplate.infrastructure.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
public class AuditPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publish(String action, String resource, String resourceId, String metadata) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        Long userId = null;
        String username = "SYSTEM";
        
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            userId = userPrincipal.getUser().getId();
            username = userPrincipal.getUsername();
        } else if (authentication != null) {
            username = authentication.getName();
        }

        String ipAddress = getClientIp();

        AuditEvent event = new AuditEvent(
            userId,
            username,
            action,
            resource,
            resourceId,
            metadata,
            ipAddress
        );

        eventPublisher.publishEvent(event);
    }
    
    // Overload for manual user context (e.g. login success/failure where context might not be fully set or different)
    public void publish(Long userId, String username, String action, String resource, String resourceId, String metadata) {
        String ipAddress = getClientIp();
        
        AuditEvent event = new AuditEvent(
            userId,
            username,
            action,
            resource,
            resourceId,
            metadata,
            ipAddress
        );

        eventPublisher.publishEvent(event);
    }

    private String getClientIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0];
            }
            return request.getRemoteAddr();
        }
        return "UNKNOWN";
    }
}
