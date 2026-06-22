package com.govtech.gsrp_backend.application.dto;

import com.govtech.gsrp_backend.domain.enums.NotificationStatus;
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
public class NotificationResponse {
    private Long id;
    private Long citizenId;
    private Long serviceRequestId;
    private ServiceType serviceType;
    private RequestStatus serviceRequestStatus;
    private String message;
    private NotificationStatus status;
    private LocalDateTime createdDate;
}
