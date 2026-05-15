package com.lawfirm.application.service;

import com.lawfirm.application.dto.request.ConflictCheckRequest;
import com.lawfirm.application.dto.request.ConflictClearRequest;
import com.lawfirm.application.dto.request.ConflictPartyRequest;
import com.lawfirm.application.dto.response.ConflictCheckResponse;
import com.lawfirm.application.dto.response.ConflictCheckResultResponse;
import com.lawfirm.application.dto.response.ConflictMatchResponse;
import com.lawfirm.application.dto.response.ConflictPartyResponse;
import com.lawfirm.application.mapper.ConflictCheckMapper;
import com.lawfirm.application.mapper.ConflictPartyMapper;
import com.lawfirm.domain.model.ConflictCheck;
import com.lawfirm.domain.model.ConflictCheck.CheckResult;
import com.lawfirm.domain.model.ConflictParty;
import com.lawfirm.domain.repository.CaseRepository;
import com.lawfirm.domain.repository.ClientRepository;
import com.lawfirm.domain.repository.ConflictCheckRepository;
import com.lawfirm.domain.repository.ConflictPartyRepository;
import com.lawfirm.domain.repository.UserRepository;
import com.lawfirm.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConflictService {

    private final ConflictPartyRepository conflictPartyRepository;
    private final ConflictCheckRepository conflictCheckRepository;
    private final ClientRepository clientRepository;
    private final CaseRepository caseRepository;
    private final UserRepository userRepository;
    private final ConflictPartyMapper conflictPartyMapper;
    private final ConflictCheckMapper conflictCheckMapper;

    // ── Conflict check ────────────────────────────────────────────────────────

    @Transactional
    public ConflictCheckResultResponse performCheck(ConflictCheckRequest request) {
        String name = request.searchName().trim();
        List<ConflictMatchResponse> matches = new ArrayList<>();

        // 1. Search existing clients
        clientRepository.searchByName(name).forEach(c -> matches.add(new ConflictMatchResponse(
            "CLIENT",
            c.getFirstName() != null ? c.getFirstName() + " " + c.getLastName() : c.getCompanyName(),
            null,
            null,
            null,
            c.getId()
        )));

        // 2. Search conflict parties across all cases
        conflictPartyRepository.searchByName(name).forEach(p -> matches.add(new ConflictMatchResponse(
            "CASE_PARTY",
            p.getName(),
            p.getPartyType().name(),
            p.getCaseEntity().getId(),
            p.getCaseEntity().getFullCaseNumber(),
            null
        )));

        // 3. Search case numbers / notes
        caseRepository.searchByName(name).forEach(c -> matches.add(new ConflictMatchResponse(
            "CASE",
            c.getFullCaseNumber(),
            null,
            c.getId(),
            c.getFullCaseNumber(),
            null
        )));

        CheckResult result = matches.isEmpty() ? CheckResult.CLEAR : CheckResult.CONFLICT_FOUND;

        var check = ConflictCheck.builder()
            .searchName(name)
            .result(result)
            .performedBy(getCurrentUser())
            .build();
        check = conflictCheckRepository.save(check);

        return new ConflictCheckResultResponse(
            check.getId(),
            name,
            !matches.isEmpty(),
            matches,
            check.getCreatedAt()
        );
    }

    // ── Clear a conflict check ─────────────────────────────────────────────────

    @Transactional
    public ConflictCheckResponse clearCheck(Long checkId, ConflictClearRequest request) {
        var check = conflictCheckRepository.findByIdWithUsers(checkId)
            .orElseThrow(() -> new ResourceNotFoundException("ConflictCheck not found: " + checkId));
        check.setClearedNote(request.clearedNote());
        check.setClearedBy(getCurrentUser());
        return conflictCheckMapper.toResponse(conflictCheckRepository.save(check));
    }

    // ── Check history ─────────────────────────────────────────────────────────

    public Page<ConflictCheckResponse> listChecks(Pageable pageable) {
        return conflictCheckRepository.findAllWithUsers(pageable)
            .map(conflictCheckMapper::toResponse);
    }

    // ── Case parties ──────────────────────────────────────────────────────────

    public List<ConflictPartyResponse> findPartiesByCase(Long caseId) {
        return conflictPartyMapper.toResponseList(conflictPartyRepository.findByCaseId(caseId));
    }

    @Transactional
    public ConflictPartyResponse addParty(Long caseId, ConflictPartyRequest request) {
        var caseEntity = caseRepository.findById(caseId)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + caseId));
        var party = ConflictParty.builder()
            .caseEntity(caseEntity)
            .partyType(request.partyType())
            .name(request.name())
            .notes(request.notes())
            .build();
        return conflictPartyMapper.toResponse(conflictPartyRepository.save(party));
    }

    @Transactional
    public void deleteParty(Long partyId) {
        if (!conflictPartyRepository.existsById(partyId)) {
            throw new ResourceNotFoundException("ConflictParty not found: " + partyId);
        }
        conflictPartyRepository.deleteById(partyId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private com.lawfirm.domain.model.User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsernameAndDeletedAtIsNull(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}
