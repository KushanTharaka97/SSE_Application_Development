package com.govtech.gsrp_backend.application.dto;

import com.govtech.gsrp_backend.domain.enums.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRequestStatusHistoryResponse {
    private Long id;
    private Long serviceRequestId;
    private RequestStatus previousStatus;
    private RequestStatus newStatus;
    private String changedBy;
    private LocalDateTime changedAt;
}
