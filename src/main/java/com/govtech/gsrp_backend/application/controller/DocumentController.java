package com.govtech.gsrp_backend.application.controller;

import com.govtech.gsrp_backend.application.dto.DocumentCreateRequest;
import com.govtech.gsrp_backend.application.dto.DocumentResponse;
import com.govtech.gsrp_backend.application.dto.DocumentUpdateRequest;
import com.govtech.gsrp_backend.application.dto.DocumentVerificationStatusUpdateRequest;
import com.govtech.gsrp_backend.domain.enums.DocumentType;
import com.govtech.gsrp_backend.domain.service.DocumentExecutionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

/**
 * REST controller for Supporting Document management.
 *
 * <p>Upload endpoint ({@code POST /api/documents}) accepts {@code multipart/form-data}
 * so that the client can submit the actual file binary alongside the metadata fields
 * (serviceRequestId, type, name) in a single request.
 *
 * <p>All other endpoints remain JSON-based.
 */
@Slf4j
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private DocumentExecutionService documentExecutionService;

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/documents   (multipart/form-data)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Upload a supporting document file and register its metadata.
     *
     * <p>Request format: {@code multipart/form-data}
     * <ul>
     *   <li>{@code file}             — the file binary (PDF, PNG, JPG, etc.)</li>
     *   <li>{@code serviceRequestId} — ID of the owning service request</li>
     *   <li>{@code type}             — DocumentType enum value (e.g. NATIONAL_ID)</li>
     *   <li>{@code name}             — human-readable display name</li>
     * </ul>
     *
     * @param file             the uploaded file part
     * @param serviceRequestId ID of the service request this document belongs to
     * @param type             document category
     * @param name             display name
     * @param principal        the authenticated citizen
     * @return the persisted {@link DocumentResponse}
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestPart("file") MultipartFile file,
            @RequestParam("serviceRequestId") @NotNull Long serviceRequestId,
            @RequestParam("type") @NotNull DocumentType type,
            @RequestParam("name") @NotBlank String name,
            Principal principal) {

        log.info("REST request to upload document | citizen: {} | serviceRequestId: {} | type: {} | file: {}",
                principal.getName(), serviceRequestId, type,
                file != null ? file.getOriginalFilename() : "null");

        DocumentCreateRequest request = DocumentCreateRequest.builder()
                .serviceRequestId(serviceRequestId)
                .type(type)
                .name(name)
                .build();

        DocumentResponse response = documentExecutionService.createDocument(request, file, principal.getName());
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/documents/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SERVICE_AGENT')")
    public ResponseEntity<DocumentResponse> getDocumentById(@PathVariable Long id) {
        log.info("REST request to get document metadata ID: {}", id);
        DocumentResponse response = documentExecutionService.getDocumentById(id);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/service-requests/{serviceRequestId}/documents
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/by-service-request/{serviceRequestId}")
    @PreAuthorize("hasAnyRole('SERVICE_AGENT', 'ADMIN')")
    public ResponseEntity<List<DocumentResponse>> getDocumentsByServiceRequest(
            @PathVariable Long serviceRequestId) {
        log.info("REST request to list documents for service request ID: {}", serviceRequestId);
        List<DocumentResponse> responses = documentExecutionService.getDocumentsByServiceRequestId(serviceRequestId);
        return ResponseEntity.ok(responses);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/documents/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SERVICE_AGENT')")
    public ResponseEntity<DocumentResponse> updateDocument(
            @PathVariable Long id,
            @Valid @RequestBody DocumentUpdateRequest request) {
        log.info("REST request to update document metadata ID: {}", id);
        DocumentResponse response = documentExecutionService.updateDocument(id, request);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /api/documents/{id}  (verification status only)
    // ─────────────────────────────────────────────────────────────────────────

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('SERVICE_AGENT')")
    public ResponseEntity<DocumentResponse> updateDocumentVerificationStatus(
            @PathVariable Long id,
            @Valid @RequestBody DocumentVerificationStatusUpdateRequest request) {
        log.info("REST request to update document verification status ID: {} → {}", id, request.getVerificationStatus());
        DocumentResponse response = documentExecutionService
                .updateDocumentVerificationStatus(id, request.getVerificationStatus());
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/documents/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        log.info("REST request to delete document ID: {}", id);
        documentExecutionService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }
}
