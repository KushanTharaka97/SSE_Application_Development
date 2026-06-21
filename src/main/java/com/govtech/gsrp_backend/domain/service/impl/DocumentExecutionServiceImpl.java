package com.govtech.gsrp_backend.domain.service.impl;

import com.govtech.gsrp_backend.application.dto.DocumentCreateRequest;
import com.govtech.gsrp_backend.application.dto.DocumentResponse;
import com.govtech.gsrp_backend.application.dto.DocumentUpdateRequest;
import com.govtech.gsrp_backend.application.exception.BusinessException;
import com.govtech.gsrp_backend.application.service.FileStorageService;
import com.govtech.gsrp_backend.domain.entity.ServiceRequest;
import com.govtech.gsrp_backend.domain.entity.SupportingDocument;
import com.govtech.gsrp_backend.domain.enums.VerificationStatus;
import com.govtech.gsrp_backend.domain.service.DocumentExecutionService;
import com.govtech.gsrp_backend.external.repository.ServiceRequestRepository;
import com.govtech.gsrp_backend.external.repository.SupportingDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Implementation of {@link DocumentExecutionService}.
 *
 * <p>Upload flow for {@code createDocument}:
 * <ol>
 *   <li>Validate that the referenced Service Request exists and belongs to the calling citizen.</li>
 *   <li>Delegate file persistence to {@link FileStorageService}, which returns a unique filename.</li>
 *   <li>Persist the document metadata (including the stored filename as {@code documentReference}) to the DB.</li>
 * </ol>
 *
 * <p>On deletion, the physical file is removed from disk before the DB record is deleted
 * to prevent orphaned files accumulating in the upload directory.
 */
@Slf4j
@Service
public class DocumentExecutionServiceImpl implements DocumentExecutionService {

    @Autowired
    private SupportingDocumentRepository supportingDocumentRepository;

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private FileStorageService fileStorageService;

    // ─────────────────────────────────────────────────────────────────────────
    // Create
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DocumentResponse createDocument(DocumentCreateRequest request,
                                           MultipartFile file,
                                           String currentUsername) {

        log.info("Attempting to create document for service request ID: {} | citizen: {} | file: {}",
                request.getServiceRequestId(), currentUsername,
                file != null ? file.getOriginalFilename() : "null");

        // 1. Validate service request exists
        ServiceRequest serviceRequest = serviceRequestRepository.findById(request.getServiceRequestId())
                .orElseThrow(() -> {
                    log.warn("Service Request not found with ID: {}", request.getServiceRequestId());
                    return new BusinessException("Service Request not found with ID: " + request.getServiceRequestId());
                });

        // 2. Ensure citizen owns the service request
        if (!serviceRequest.getCitizenReference().getNic().equals(currentUsername)) {
            log.warn("Access denied: citizen '{}' attempted to upload document for service request ID: {} owned by '{}'",
                    currentUsername, request.getServiceRequestId(),
                    serviceRequest.getCitizenReference().getNic());
            throw new AccessDeniedException("You can only add documents to your own service requests.");
        }

        // 3. Persist the file to local storage – returns the unique stored filename
        String storedFilename = fileStorageService.storeFile(file);
        log.info("File stored on disk as: {}", storedFilename);

        // 4. Persist metadata to DB
        SupportingDocument document = SupportingDocument.builder()
                .requestReference(serviceRequest)
                .type(request.getType())
                .name(request.getName().trim())
                .documentReference(storedFilename)          // path stored in DB
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        document = supportingDocumentRepository.save(document);
        log.info("Document record saved with ID: {} | stored file: {}", document.getId(), storedFilename);

        return mapToResponse(document);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Read
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(Long id) {
        log.info("Fetching document metadata for ID: {}", id);
        return mapToResponse(getDocumentEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByServiceRequestId(Long serviceRequestId) {
        log.info("Fetching all documents for service request ID: {}", serviceRequestId);

        if (!serviceRequestRepository.existsById(serviceRequestId)) {
            throw new BusinessException("Service Request not found with ID: " + serviceRequestId);
        }

        return supportingDocumentRepository
                .findByRequestReferenceIdOrderByIdAsc(serviceRequestId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Update
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DocumentResponse updateDocument(Long id, DocumentUpdateRequest request) {
        log.info("Updating document metadata for ID: {}", id);
        SupportingDocument document = getDocumentEntityById(id);

        document.setType(request.getType());
        document.setName(request.getName().trim());
        document.setDocumentReference(request.getDocumentReference().trim());
        document.setVerificationStatus(request.getVerificationStatus());

        document = supportingDocumentRepository.save(document);
        log.info("Document metadata updated for ID: {}", id);
        return mapToResponse(document);
    }

    @Override
    @Transactional
    public DocumentResponse updateDocumentVerificationStatus(Long id, VerificationStatus verificationStatus) {
        log.info("Updating verification status for document ID: {} → {}", id, verificationStatus);
        SupportingDocument document = getDocumentEntityById(id);

        document.setVerificationStatus(verificationStatus);
        document = supportingDocumentRepository.save(document);

        log.info("Verification status updated for document ID: {}", id);
        return mapToResponse(document);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delete
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteDocument(Long id) {
        log.info("Deleting document with ID: {}", id);
        SupportingDocument document = getDocumentEntityById(id);

        // Remove physical file from disk first (before DB record disappears)
        fileStorageService.deleteFile(document.getDocumentReference());

        supportingDocumentRepository.delete(document);
        log.info("Document record deleted from DB for ID: {}", id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private SupportingDocument getDocumentEntityById(Long id) {
        return supportingDocumentRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Supporting Document not found with ID: {}", id);
                    return new BusinessException("Supporting Document not found with ID: " + id);
                });
    }

    private DocumentResponse mapToResponse(SupportingDocument document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .serviceRequestId(document.getRequestReference().getId())
                .citizenId(document.getRequestReference().getCitizenReference().getId())
                .citizenName(document.getRequestReference().getCitizenReference().getName())
                .serviceType(document.getRequestReference().getServiceType())
                .type(document.getType())
                .name(document.getName())
                .documentReference(document.getDocumentReference())
                .verificationStatus(document.getVerificationStatus())
                .build();
    }
}
