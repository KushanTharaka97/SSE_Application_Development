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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentExecutionServiceImpl implements DocumentExecutionService {

    private final SupportingDocumentRepository supportingDocumentRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public DocumentResponse createDocument(DocumentCreateRequest request, MultipartFile file, String currentUsername) {
        log.info("Attempting to create document for service request ID: {} | citizen: {} | file: {}",
                request.getServiceRequestId(), currentUsername, file != null ? file.getOriginalFilename() : "null");

        ServiceRequest serviceRequest = serviceRequestRepository.findById(request.getServiceRequestId())
                .orElseThrow(() -> {
                    log.warn("Service request not found with ID: {}", request.getServiceRequestId());
                    return new BusinessException("Service Request not found with ID: " + request.getServiceRequestId());
                });

        if (!serviceRequest.getCitizenReference().getNic().equals(currentUsername)) {
            log.warn("Access denied: citizen '{}' attempted to upload document for service request ID: {} owned by '{}'",
                    currentUsername, request.getServiceRequestId(), serviceRequest.getCitizenReference().getNic());
            throw new AccessDeniedException("You can only add documents to your own service requests.");
        }

        String storedFilename = fileStorageService.storeFile(file);
        log.info("File stored on disk as: {}", storedFilename);

        SupportingDocument document = SupportingDocument.builder()
                .requestReference(serviceRequest)
                .type(request.getType())
                .name(request.getName().trim())
                .documentReference(storedFilename)
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        document = supportingDocumentRepository.save(document);
        log.info("Document record saved with ID: {} | stored file: {}", document.getId(), storedFilename);
        return mapToResponse(document);
    }

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

        return supportingDocumentRepository.findByRequestReferenceIdOrderByIdAsc(serviceRequestId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

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
        log.info("Updating verification status for document ID: {} to {}", id, verificationStatus);
        SupportingDocument document = getDocumentEntityById(id);

        document.setVerificationStatus(verificationStatus);
        document = supportingDocumentRepository.save(document);

        log.info("Verification status updated for document ID: {}", id);
        return mapToResponse(document);
    }

    @Override
    @Transactional
    public void deleteDocument(Long id) {
        log.info("Deleting document with ID: {}", id);
        SupportingDocument document = getDocumentEntityById(id);

        fileStorageService.deleteFile(document.getDocumentReference());
        supportingDocumentRepository.delete(document);
        log.info("Document record deleted from DB for ID: {}", id);
    }

    private SupportingDocument getDocumentEntityById(Long id) {
        return supportingDocumentRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Supporting document not found with ID: {}", id);
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
