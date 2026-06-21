package com.govtech.gsrp_backend.application.dto;

import com.govtech.gsrp_backend.domain.enums.RequestStatus;
import com.govtech.gsrp_backend.domain.enums.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRequestResponse {
    private Long id;
    private Long citizenId;
    private String citizenName;
    private ServiceType serviceType;
    private String description;
    private RequestStatus status;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
