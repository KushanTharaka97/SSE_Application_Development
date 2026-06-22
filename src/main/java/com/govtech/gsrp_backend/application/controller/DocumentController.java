package com.govtech.gsrp_backend.application.controller;

import com.govtech.gsrp_backend.application.dto.ApiResponseFactory;
import com.govtech.gsrp_backend.application.dto.ApiSuccessResponse;
import com.govtech.gsrp_backend.application.dto.DocumentCreateRequest;
import com.govtech.gsrp_backend.application.dto.DocumentResponse;
import com.govtech.gsrp_backend.application.dto.DocumentUpdateRequest;
import com.govtech.gsrp_backend.application.dto.DocumentVerificationStatusUpdateRequest;
import com.govtech.gsrp_backend.domain.enums.DocumentType;
import com.govtech.gsrp_backend.domain.service.DocumentExecutionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentExecutionService documentExecutionService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<ApiSuccessResponse<DocumentResponse>> uploadDocument(
            @RequestPart("file") MultipartFile file,
            @RequestParam("serviceRequestId") @NotNull Long serviceRequestId,
            @RequestParam("type") @NotNull DocumentType type,
            @RequestParam("name") @NotBlank String name,
            Principal principal) {

        log.info("REST request to upload document | citizen: {} | serviceRequestId: {} | type: {} | file: {}",
                principal.getName(), serviceRequestId, type, file != null ? file.getOriginalFilename() : "null");

        DocumentCreateRequest request = DocumentCreateRequest.builder()
                .serviceRequestId(serviceRequestId)
                .type(type)
                .name(name)
                .build();

        DocumentResponse response = documentExecutionService.createDocument(request, file, principal.getName());
        return ApiResponseFactory.created("Supporting document uploaded successfully.", response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SERVICE_AGENT')")
    public ResponseEntity<ApiSuccessResponse<DocumentResponse>> getDocumentById(@PathVariable Long id) {
        log.info("REST request to get document metadata ID: {}", id);
        DocumentResponse response = documentExecutionService.getDocumentById(id);
        return ApiResponseFactory.ok("Supporting document retrieved successfully.", response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SERVICE_AGENT')")
    public ResponseEntity<ApiSuccessResponse<DocumentResponse>> updateDocument(
            @PathVariable Long id,
            @Valid @RequestBody DocumentUpdateRequest request) {
        log.info("REST request to update document metadata ID: {}", id);
        DocumentResponse response = documentExecutionService.updateDocument(id, request);
        return ApiResponseFactory.ok("Supporting document updated successfully.", response);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('SERVICE_AGENT')")
    public ResponseEntity<ApiSuccessResponse<DocumentResponse>> updateDocumentVerificationStatus(
            @PathVariable Long id,
            @Valid @RequestBody DocumentVerificationStatusUpdateRequest request) {
        log.info("REST request to update document verification status ID: {} to {}", id, request.getVerificationStatus());
        DocumentResponse response = documentExecutionService.updateDocumentVerificationStatus(id, request.getVerificationStatus());
        return ApiResponseFactory.ok("Supporting document verification status updated successfully.", response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<Void>> deleteDocument(@PathVariable Long id) {
        log.info("REST request to delete document ID: {}", id);
        documentExecutionService.deleteDocument(id);
        return ApiResponseFactory.ok("Supporting document deleted successfully.");
    }
}
