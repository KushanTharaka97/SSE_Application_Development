package com.govtech.gsrp_backend.domain.service;

import com.govtech.gsrp_backend.application.dto.ServiceRequestResponse;
import com.govtech.gsrp_backend.application.dto.ServiceRequestSubmitDTO;
import com.govtech.gsrp_backend.domain.enums.RequestStatus;
import com.govtech.gsrp_backend.domain.enums.ServiceType;
import org.springframework.data.domain.Page;

public interface ServiceRequestExecutionProcessService {
    ServiceRequestResponse submitRequest(ServiceRequestSubmitDTO submitDTO, String currentUsername);
    Page<ServiceRequestResponse> getMyRequests(String currentUsername, int page, int size);
    ServiceRequestResponse getRequestById(Long id, String currentUsername);
    Page<ServiceRequestResponse> getAllRequests(Long citizenId, RequestStatus status, ServiceType serviceType, int page, int size);
    ServiceRequestResponse updateRequestDetails(Long id, ServiceRequestSubmitDTO submitDTO);
    ServiceRequestResponse updateRequestStatus(Long id, RequestStatus status);
    void cancelRequest(Long id);
}
