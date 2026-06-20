package com.govtech.gsrp_backend.application.dto;

import com.govtech.gsrp_backend.domain.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new Supporting Document record.
 * The actual file binary is received separately as a {@link org.springframework.web.multipart.MultipartFile}
 * via the multipart/form-data request and is NOT part of this DTO.
 *
 * Fields:
 * - serviceRequestId : the ID of the Service Request this document belongs to
 * - type             : the document category (e.g. NATIONAL_ID, INCOME_PROOF)
 * - name             : a human-readable display name for the document
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentCreateRequest {

    @NotNull(message = "Service request reference is mandatory")
    private Long serviceRequestId;

    @NotNull(message = "Document type is mandatory")
    private DocumentType type;

    @NotBlank(message = "Document name is mandatory")
    @Size(max = 255)
    private String name;
}
