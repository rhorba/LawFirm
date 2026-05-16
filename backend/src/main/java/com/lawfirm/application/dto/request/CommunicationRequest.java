package com.lawfirm.application.dto.request;

import com.lawfirm.domain.model.Communication.CommunicationDirection;
import com.lawfirm.domain.model.Communication.CommunicationType;
import jakarta.validation.constraints.NotNull;

public record CommunicationRequest(
    @NotNull CommunicationType commType,
    @NotNull CommunicationDirection direction,
    String subject,
    String body,
    String recipientEmail,
    String recipientPhone
) {}
