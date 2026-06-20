package com.govtech.gsrp_backend.application.dto;

import com.govtech.gsrp_backend.domain.enums.VerificationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentVerificationStatusUpdateRequest {
    @NotNull(message = "Verification status is mandatory")
    private VerificationStatus verificationStatus;
}
