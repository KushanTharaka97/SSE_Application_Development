package com.govtech.gsrp_backend.application.dto;

import com.govtech.gsrp_backend.domain.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @NotBlank(message = "Document reference is mandatory")
    @Size(max = 255)
    private String documentReference;
}
