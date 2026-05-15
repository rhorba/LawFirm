package com.lawfirm.presentation.controller;

import com.lawfirm.application.dto.response.DocumentResponse;
import com.lawfirm.application.service.DocumentService;
import com.lawfirm.application.service.DocumentStorageService;
import com.lawfirm.domain.model.DocumentCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentStorageService storageService;

    @GetMapping("/cases/{caseId}/documents")
    @PreAuthorize("hasPermission(null, 'DOCUMENT_READ')")
    public ResponseEntity<List<DocumentResponse>> listByCaseId(
        @PathVariable Long caseId,
        @RequestParam(required = false) String q
    ) {
        List<DocumentResponse> result = (q != null && !q.isBlank())
            ? documentService.searchByCaseId(caseId, q)
            : documentService.findByCaseId(caseId);
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/cases/{caseId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasPermission(null, 'DOCUMENT_CREATE')")
    public ResponseEntity<DocumentResponse> upload(
        @PathVariable Long caseId,
        @RequestParam("file") MultipartFile file,
        @RequestParam("title") String title,
        @RequestParam(value = "description", required = false) String description,
        @RequestParam("category") DocumentCategory category
    ) {
        DocumentResponse response = documentService.create(caseId, file, title, description, category);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/documents/{id}")
    @PreAuthorize("hasPermission(null, 'DOCUMENT_READ')")
    public ResponseEntity<DocumentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.findById(id));
    }

    @GetMapping("/documents/{id}/download")
    @PreAuthorize("hasPermission(null, 'DOCUMENT_READ')")
    public ResponseEntity<Resource> download(
        @PathVariable Long id,
        @RequestParam(defaultValue = "false") boolean inline
    ) {
        DocumentResponse meta = documentService.findById(id);
        Resource resource = documentService.download(id);

        boolean preview = inline && storageService.isInlinePreviewable(meta.contentType());
        ContentDisposition disposition = preview
            ? ContentDisposition.inline().filename(meta.originalFilename()).build()
            : ContentDisposition.attachment().filename(meta.originalFilename()).build();

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(meta.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .body(resource);
    }

    @DeleteMapping("/documents/{id}")
    @PreAuthorize("hasPermission(null, 'DOCUMENT_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
