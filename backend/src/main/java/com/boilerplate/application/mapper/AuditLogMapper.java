package com.boilerplate.application.mapper;

import com.boilerplate.application.dto.response.AuditLogResponse;
import com.boilerplate.domain.model.AuditLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {
    AuditLogResponse toResponse(AuditLog auditLog);
}
