package com.lawfirm.application.mapper;

import com.lawfirm.application.dto.response.AuditLogResponse;
import com.lawfirm.domain.model.AuditLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {
    AuditLogResponse toResponse(AuditLog auditLog);
}
