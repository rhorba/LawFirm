package com.lawfirm.presentation.controller;

import com.lawfirm.application.dto.request.ConflictCheckRequest;
import com.lawfirm.application.dto.request.ConflictClearRequest;
import com.lawfirm.application.dto.request.ConflictPartyRequest;
import com.lawfirm.application.dto.response.ConflictCheckResponse;
import com.lawfirm.application.dto.response.ConflictCheckResultResponse;
import com.lawfirm.application.dto.response.ConflictPartyResponse;
import com.lawfirm.application.service.ConflictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/api/conflicts")
@RequiredArgsConstructor
@Tag(name = "Conflict Checking", description = "Client conflict of interest checking")
public class ConflictController {

    private final ConflictService conflictService;

    // ── Conflict check ────────────────────────────────────────────────────────

    @PostMapping("/check")
    @PreAuthorize("hasPermission(null, 'CONFLICT_CREATE')")
    @Operation(summary = "Perform a conflict check by name")
    public ResponseEntity<ConflictCheckResultResponse> check(
            @Valid @RequestBody ConflictCheckRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(conflictService.performCheck(request));
    }

    @GetMapping("/history")
    @PreAuthorize("hasPermission(null, 'CONFLICT_READ')")
    @Operation(summary = "List past conflict checks (paginated)")
    public ResponseEntity<Page<ConflictCheckResponse>> history(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(conflictService.listChecks(pageable));
    }

    @PostMapping("/history/{id}/clear")
    @PreAuthorize("hasPermission(null, 'CONFLICT_MANAGE')")
    @Operation(summary = "Mark a conflict check as cleared with a note")
    public ResponseEntity<ConflictCheckResponse> clear(
            @PathVariable Long id,
            @Valid @RequestBody ConflictClearRequest request) {
        return ResponseEntity.ok(conflictService.clearCheck(id, request));
    }

    // ── Case parties ──────────────────────────────────────────────────────────

    @GetMapping("/cases/{caseId}/parties")
    @PreAuthorize("hasPermission(null, 'CONFLICT_READ')")
    @Operation(summary = "List conflict parties for a case")
    public ResponseEntity<List<ConflictPartyResponse>> listParties(@PathVariable Long caseId) {
        return ResponseEntity.ok(conflictService.findPartiesByCase(caseId));
    }

    @PostMapping("/cases/{caseId}/parties")
    @PreAuthorize("hasPermission(null, 'CONFLICT_CREATE')")
    @Operation(summary = "Add a party to a case for conflict tracking")
    public ResponseEntity<ConflictPartyResponse> addParty(
            @PathVariable Long caseId,
            @Valid @RequestBody ConflictPartyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(conflictService.addParty(caseId, request));
    }

    @DeleteMapping("/parties/{partyId}")
    @PreAuthorize("hasPermission(null, 'CONFLICT_MANAGE')")
    @Operation(summary = "Remove a party from conflict tracking")
    public ResponseEntity<Void> deleteParty(@PathVariable Long partyId) {
        conflictService.deleteParty(partyId);
        return ResponseEntity.noContent().build();
    }
}
