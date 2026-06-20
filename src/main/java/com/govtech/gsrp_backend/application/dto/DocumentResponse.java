package com.govtech.gsrp_backend.application.dto;

import com.govtech.gsrp_backend.domain.enums.DocumentType;
import com.govtech.gsrp_backend.domain.enums.ServiceType;
import com.govtech.gsrp_backend.domain.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponse {
    private Long id;
    private Long serviceRequestId;
    private Long citizenId;
    private String citizenName;
    private ServiceType serviceType;
    private DocumentType type;
    private String name;
    private String documentReference;
    private VerificationStatus verificationStatus;
}
