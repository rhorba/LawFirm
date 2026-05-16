package com.lawfirm.presentation.controller;

import com.lawfirm.application.dto.request.CommunicationRequest;
import com.lawfirm.application.dto.request.EmailSendRequest;
import com.lawfirm.application.dto.response.CommunicationResponse;
import com.lawfirm.application.service.CommunicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommunicationController {

    private final CommunicationService communicationService;

    @GetMapping("/cases/{caseId}/communications")
    @PreAuthorize("hasPermission(null, 'COMMUNICATION_READ')")
    public ResponseEntity<List<CommunicationResponse>> listByCaseId(@PathVariable Long caseId) {
        return ResponseEntity.ok(communicationService.findByCaseId(caseId));
    }

    @PostMapping("/cases/{caseId}/communications")
    @PreAuthorize("hasPermission(null, 'COMMUNICATION_CREATE')")
    public ResponseEntity<CommunicationResponse> log(
        @PathVariable Long caseId,
        @Valid @RequestBody CommunicationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(communicationService.log(caseId, request));
    }

    @PostMapping("/cases/{caseId}/communications/send-email")
    @PreAuthorize("hasPermission(null, 'COMMUNICATION_CREATE')")
    public ResponseEntity<CommunicationResponse> sendEmail(
        @PathVariable Long caseId,
        @Valid @RequestBody EmailSendRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(communicationService.sendEmail(caseId, request));
    }

    @DeleteMapping("/communications/{id}")
    @PreAuthorize("hasPermission(null, 'COMMUNICATION_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        communicationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
