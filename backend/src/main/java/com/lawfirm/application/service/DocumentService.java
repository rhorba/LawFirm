package com.lawfirm.application.service;

import com.lawfirm.application.dto.response.DocumentResponse;
import com.lawfirm.application.mapper.DocumentMapper;
import com.lawfirm.domain.model.Case;
import com.lawfirm.domain.model.Document;
import com.lawfirm.domain.model.DocumentCategory;
import com.lawfirm.domain.model.User;
import com.lawfirm.domain.repository.CaseRepository;
import com.lawfirm.domain.repository.DocumentRepository;
import com.lawfirm.domain.repository.UserRepository;
import com.lawfirm.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final CaseRepository caseRepository;
    private final UserRepository userRepository;
    private final DocumentStorageService storageService;
    private final DocumentMapper documentMapper;

    public List<DocumentResponse> findByCaseId(Long caseId) {
        return documentRepository.findByCaseId(caseId).stream()
            .map(documentMapper::toResponse)
            .toList();
    }

    public List<DocumentResponse> searchByCaseId(Long caseId, String query) {
        return documentRepository.searchByCaseIdAndQuery(caseId, query).stream()
            .map(documentMapper::toResponse)
            .toList();
    }

    public DocumentResponse findById(Long id) {
        Document doc = documentRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
        return documentMapper.toResponse(doc);
    }

    @Transactional
    public DocumentResponse create(Long caseId, MultipartFile file, String title, String description, DocumentCategory category) {
        Case caseEntity = caseRepository.findById(caseId)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + caseId));
        User uploader = currentUser();

        String storedFilename = storageService.store(file);

        Document doc = Document.builder()
            .caseEntity(caseEntity)
            .title(title)
            .description(description)
            .category(category)
            .originalFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file")
            .storedFilename(storedFilename)
            .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
            .fileSize(file.getSize())
            .uploadedBy(uploader)
            .build();

        return documentMapper.toResponse(documentRepository.save(doc));
    }

    public Resource download(Long id) {
        Document doc = documentRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
        return storageService.load(doc.getStoredFilename());
    }

    @Transactional
    public void delete(Long id) {
        Document doc = documentRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
        storageService.delete(doc.getStoredFilename());
        documentRepository.delete(doc);
    }

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsernameAndDeletedAtIsNull(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}
