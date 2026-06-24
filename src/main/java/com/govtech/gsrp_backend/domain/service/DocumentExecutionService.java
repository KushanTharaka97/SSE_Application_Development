package com.govtech.gsrp_backend.domain.service;

import com.govtech.gsrp_backend.application.dto.DocumentCreateRequest;
import com.govtech.gsrp_backend.application.dto.DocumentFileResponse;
import com.govtech.gsrp_backend.application.dto.DocumentResponse;
import com.govtech.gsrp_backend.application.dto.DocumentUpdateRequest;
import com.govtech.gsrp_backend.domain.enums.VerificationStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Domain service contract for all Supporting Document operations.
 * <p>
 * {@code createDocument} now accepts the actual file binary via {@link MultipartFile}
 * in addition to the metadata DTO.  The implementation is responsible for persisting
 * the file to local storage (via {@link com.govtech.gsrp_backend.application.service.FileStorageService})
 * and recording the resulting path in the {@code documentReference} column.
 */
public interface DocumentExecutionService {

    /**
     * Stores the uploaded file on disk and persists document metadata to the database.
     *
     * @param request         metadata (serviceRequestId, type, name)
     * @param file            the actual file binary uploaded by the citizen
     * @param currentUsername the NIC / username of the authenticated citizen
     * @return the persisted document response
     */
    DocumentResponse createDocument(DocumentCreateRequest request, MultipartFile file, String currentUsername);

    DocumentResponse getDocumentById(Long id, String currentUsername);

    List<DocumentResponse> getDocumentsByServiceRequestId(Long serviceRequestId, String currentUsername);

    DocumentFileResponse getDocumentFile(Long id, String currentUsername);

    DocumentResponse updateDocument(Long id, DocumentUpdateRequest request);

    DocumentResponse updateDocumentVerificationStatus(Long id, VerificationStatus verificationStatus);

    /**
     * Deletes the document metadata from the database AND removes the physical file from disk.
     *
     * @param id the document ID
     */
    void deleteDocument(Long id);
}
