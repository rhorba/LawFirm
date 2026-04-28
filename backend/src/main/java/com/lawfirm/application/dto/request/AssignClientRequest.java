package com.lawfirm.application.dto.request;

public record AssignClientRequest(Long clientId) {
    // clientId = null means unassign client from case
}
