package com.lawfirm.application.dto.response;

import com.lawfirm.domain.model.Communication.CommunicationDirection;
import com.lawfirm.domain.model.Communication.CommunicationType;

import java.time.LocalDateTime;

public record CommunicationResponse(
    Long id,
    Long caseId,
    String caseNumber,
    CommunicationType commType,
    CommunicationDirection direction,
    String subject,
    String body,
    String recipientEmail,
    String recipientPhone,
    String sentByUsername,
    LocalDateTime createdAt
) {}
