package com.lawfirm.presentation.controller;

import com.lawfirm.application.dto.request.CreateClientRequest;
import com.lawfirm.application.dto.request.UpdateClientRequest;
import com.lawfirm.application.dto.response.ClientResponse;
import com.lawfirm.application.dto.response.ClientSummary;
import com.lawfirm.application.service.ClientExportService;
import com.lawfirm.application.service.ClientService;
import com.lawfirm.domain.model.ClientType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@Tag(name = "Clients", description = "Client management")
public class ClientController {

    private final ClientService clientService;
    private final ClientExportService clientExportService;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'CLIENT_READ')")
    @Operation(summary = "Search/list clients")
    public ResponseEntity<Page<ClientSummary>> search(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) ClientType type,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(clientService.search(search, type, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'CLIENT_READ')")
    @Operation(summary = "Get client by ID")
    public ResponseEntity<ClientResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'CLIENT_CREATE')")
    @Operation(summary = "Create client")
    public ResponseEntity<ClientResponse> create(
        @Valid @RequestBody CreateClientRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'CLIENT_UPDATE')")
    @Operation(summary = "Update client")
    public ResponseEntity<ClientResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody UpdateClientRequest request
    ) {
        return ResponseEntity.ok(clientService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'CLIENT_DELETE')")
    @Operation(summary = "Soft-deactivate client")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        clientService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export")
    @PreAuthorize("hasPermission(null, 'CLIENT_READ')")
    @Operation(summary = "Export clients to Excel")
    public void export(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) ClientType type,
        HttpServletResponse response
    ) throws IOException {
        byte[] xlsx = clientExportService.export(search, type);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=clients-export.xlsx");
        response.getOutputStream().write(xlsx);
    }
}
