package com.govtech.gsrp_backend.application.controller;

import com.govtech.gsrp_backend.application.dto.ApiResponseFactory;
import com.govtech.gsrp_backend.application.dto.ApiSuccessResponse;
import com.govtech.gsrp_backend.application.dto.DocumentResponse;
import com.govtech.gsrp_backend.application.dto.ServiceRequestResponse;
import com.govtech.gsrp_backend.application.dto.ServiceRequestStatusHistoryResponse;
import com.govtech.gsrp_backend.application.dto.ServiceRequestSubmitDTO;
import com.govtech.gsrp_backend.application.dto.StatusUpdateRequest;
import com.govtech.gsrp_backend.domain.enums.RequestStatus;
import com.govtech.gsrp_backend.domain.enums.ServiceType;
import com.govtech.gsrp_backend.domain.service.DocumentExecutionService;
import com.govtech.gsrp_backend.domain.service.ServiceRequestExecutionProcessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/service-requests")
@RequiredArgsConstructor
public class ServiceRequestController {

    private final ServiceRequestExecutionProcessService serviceRequestService;
    private final DocumentExecutionService documentExecutionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CITIZEN', 'ADMIN')")
    public ResponseEntity<ApiSuccessResponse<ServiceRequestResponse>> submitRequest(
            @Valid @RequestBody ServiceRequestSubmitDTO submitDTO,
            Principal principal) {
        log.info("REST request to submit service request. Caller: {}", principal.getName());
        ServiceRequestResponse response = serviceRequestService.submitRequest(submitDTO, principal.getName());
        return ApiResponseFactory.created("Service request submitted successfully.", response);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<ApiSuccessResponse<Page<ServiceRequestResponse>>> getMyRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal) {
        log.info("REST request to get my service requests page: {}, size: {}, caller: {}", page, size, principal.getName());
        Page<ServiceRequestResponse> response = serviceRequestService.getMyRequests(principal.getName(), page, size);
        return ApiResponseFactory.ok("Service requests retrieved successfully.", response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CITIZEN', 'SERVICE_AGENT', 'ADMIN')")
    public ResponseEntity<ApiSuccessResponse<ServiceRequestResponse>> getRequestById(
            @PathVariable Long id,
            Principal principal) {
        log.info("REST request to get service request ID: {}, caller: {}", id, principal.getName());
        ServiceRequestResponse response = serviceRequestService.getRequestById(id, principal.getName());
        return ApiResponseFactory.ok("Service request retrieved successfully.", response);
    }

    @GetMapping("/{id}/documents")
    @PreAuthorize("hasRole('SERVICE_AGENT')")
    public ResponseEntity<ApiSuccessResponse<List<DocumentResponse>>> getRequestDocuments(@PathVariable Long id) {
        log.info("REST request to get documents for service request ID: {}", id);
        List<DocumentResponse> response = documentExecutionService.getDocumentsByServiceRequestId(id);
        return ApiResponseFactory.ok("Supporting documents retrieved successfully.", response);
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasRole('SERVICE_AGENT')")
    public ResponseEntity<ApiSuccessResponse<List<ServiceRequestStatusHistoryResponse>>> getRequestStatusHistory(@PathVariable Long id) {
        log.info("REST request to get status history for service request ID: {}", id);
        List<ServiceRequestStatusHistoryResponse> response = serviceRequestService.getStatusHistory(id);
        return ApiResponseFactory.ok("Service request status history retrieved successfully.", response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SERVICE_AGENT', 'ADMIN')")
    public ResponseEntity<ApiSuccessResponse<Page<ServiceRequestResponse>>> getAllRequests(
            @RequestParam(required = false) Long citizenId,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) ServiceType serviceType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("REST request to get all service requests - citizenId: {}, status: {}, serviceType: {}, page: {}, size: {}",
                citizenId, status, serviceType, page, size);
        Page<ServiceRequestResponse> response = serviceRequestService.getAllRequests(citizenId, status, serviceType, page, size);
        return ApiResponseFactory.ok("Service requests retrieved successfully.", response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SERVICE_AGENT', 'ADMIN')")
    public ResponseEntity<ApiSuccessResponse<ServiceRequestResponse>> updateRequestDetails(
            @PathVariable Long id,
            @Valid @RequestBody ServiceRequestSubmitDTO submitDTO) {
        log.info("REST request to update service request ID: {}", id);
        ServiceRequestResponse response = serviceRequestService.updateRequestDetails(id, submitDTO);
        return ApiResponseFactory.ok("Service request updated successfully.", response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SERVICE_AGENT', 'ADMIN')")
    public ResponseEntity<ApiSuccessResponse<ServiceRequestResponse>> updateRequestStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest statusUpdate,
            Principal principal) {
        log.info("REST request to update service request ID: {} status to: {} by caller: {}", id, statusUpdate.getStatus(), principal.getName());
        ServiceRequestResponse response = serviceRequestService.updateRequestStatus(id, statusUpdate.getStatus(), principal.getName());
        return ApiResponseFactory.ok("Service request status updated successfully.", response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<Void>> cancelRequest(@PathVariable Long id, Principal principal) {
        log.info("REST request to cancel service request ID: {} by caller: {}", id, principal.getName());
        serviceRequestService.cancelRequest(id, principal.getName());
        return ApiResponseFactory.ok("Service request cancelled successfully.");
    }
}
