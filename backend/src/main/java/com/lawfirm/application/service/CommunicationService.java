package com.lawfirm.application.service;

import com.lawfirm.application.dto.request.CommunicationRequest;
import com.lawfirm.application.dto.request.EmailSendRequest;
import com.lawfirm.application.dto.response.CommunicationResponse;
import com.lawfirm.application.mapper.CommunicationMapper;
import com.lawfirm.domain.model.Case;
import com.lawfirm.domain.model.Communication;
import com.lawfirm.domain.model.Communication.CommunicationDirection;
import com.lawfirm.domain.model.Communication.CommunicationType;
import com.lawfirm.domain.model.User;
import com.lawfirm.domain.repository.CaseRepository;
import com.lawfirm.domain.repository.CommunicationRepository;
import com.lawfirm.domain.repository.UserRepository;
import com.lawfirm.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunicationService {

    private final CommunicationRepository communicationRepository;
    private final CaseRepository caseRepository;
    private final UserRepository userRepository;
    private final EmailSenderService emailSenderService;
    private final CommunicationMapper communicationMapper;

    public List<CommunicationResponse> findByCaseId(Long caseId) {
        return communicationRepository.findByCaseId(caseId).stream()
            .map(communicationMapper::toResponse)
            .toList();
    }

    @Transactional
    public CommunicationResponse log(Long caseId, CommunicationRequest request) {
        Case caseEntity = caseRepository.findById(caseId)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + caseId));
        User sender = currentUser();

        Communication comm = Communication.builder()
            .caseEntity(caseEntity)
            .commType(request.commType())
            .direction(request.direction())
            .subject(request.subject())
            .body(request.body())
            .recipientEmail(request.recipientEmail())
            .recipientPhone(request.recipientPhone())
            .sentBy(sender)
            .build();

        return communicationMapper.toResponse(communicationRepository.save(comm));
    }

    @Transactional
    public CommunicationResponse sendEmail(Long caseId, EmailSendRequest request) {
        Case caseEntity = caseRepository.findById(caseId)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + caseId));
        User sender = currentUser();

        emailSenderService.send(request.to(), request.subject(), request.body());

        Communication comm = Communication.builder()
            .caseEntity(caseEntity)
            .commType(CommunicationType.EMAIL_SENT)
            .direction(CommunicationDirection.OUTBOUND)
            .subject(request.subject())
            .body(request.body())
            .recipientEmail(request.to())
            .sentBy(sender)
            .build();

        return communicationMapper.toResponse(communicationRepository.save(comm));
    }

    @Transactional
    public void delete(Long id) {
        Communication comm = communicationRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException("Communication not found: " + id));
        communicationRepository.delete(comm);
    }

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsernameAndDeletedAtIsNull(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}
