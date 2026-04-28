package com.lawfirm.application.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.application.dto.response.AuditLogResponse;
import com.lawfirm.domain.model.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

@Mapper(componentModel = "spring")
public abstract class AuditLogMapper {

    @Autowired
    protected ObjectMapper objectMapper;

    @Mapping(target = "oldValues", expression = "java(parseJson(auditLog.getOldValues()))")
    @Mapping(target = "newValues", expression = "java(parseJson(auditLog.getNewValues()))")
    public abstract AuditLogResponse toResponse(AuditLog auditLog);

    protected Map<String, Object> parseJson(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }
}
