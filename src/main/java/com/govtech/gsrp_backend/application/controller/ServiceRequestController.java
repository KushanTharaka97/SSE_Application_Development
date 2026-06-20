package com.govtech.gsrp_backend.application.controller;

import com.govtech.gsrp_backend.application.dto.ServiceRequestResponse;
import com.govtech.gsrp_backend.application.dto.ServiceRequestSubmitDTO;
import com.govtech.gsrp_backend.application.dto.StatusUpdateRequest;
import com.govtech.gsrp_backend.domain.enums.RequestStatus;
import com.govtech.gsrp_backend.domain.enums.ServiceType;
import com.govtech.gsrp_backend.domain.service.ServiceRequestExecutionProcessService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Slf4j
@RestController
@RequestMapping("/api/service-requests")
public class ServiceRequestController {

    @Autowired
    private ServiceRequestExecutionProcessService serviceRequestService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CITIZEN', 'ADMIN')")
    public ResponseEntity<ServiceRequestResponse> submitRequest(
            @Valid @RequestBody ServiceRequestSubmitDTO submitDTO,
            Principal principal) {
        log.info("REST request to submit Service Request. Caller: {}", principal.getName());
        ServiceRequestResponse response = serviceRequestService.submitRequest(submitDTO, principal.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<Page<ServiceRequestResponse>> getMyRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal) {
        log.info("REST request to get my Service Requests page: {}, size: {}, Caller: {}", page, size, principal.getName());
        Page<ServiceRequestResponse> response = serviceRequestService.getMyRequests(principal.getName(), page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CITIZEN', 'SERVICE_AGENT', 'ADMIN')")
    public ResponseEntity<ServiceRequestResponse> getRequestById(
            @PathVariable Long id,
            Principal principal) {
        log.info("REST request to get Service Request ID: {}, Caller: {}", id, principal.getName());
        ServiceRequestResponse response = serviceRequestService.getRequestById(id, principal.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SERVICE_AGENT', 'ADMIN')")
    public ResponseEntity<Page<ServiceRequestResponse>> getAllRequests(
            @RequestParam(required = false) Long citizenId,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) ServiceType serviceType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("REST request to get all Service Requests - citizenId: {}, status: {}, serviceType: {}, page: {}, size: {}", 
                citizenId, status, serviceType, page, size);
        Page<ServiceRequestResponse> response = serviceRequestService.getAllRequests(citizenId, status, serviceType, page, size);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SERVICE_AGENT', 'ADMIN')")
    public ResponseEntity<ServiceRequestResponse> updateRequestDetails(
            @PathVariable Long id,
            @Valid @RequestBody ServiceRequestSubmitDTO submitDTO) {
        log.info("REST request to update Service Request ID: {}", id);
        ServiceRequestResponse response = serviceRequestService.updateRequestDetails(id, submitDTO);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SERVICE_AGENT', 'ADMIN')")
    public ResponseEntity<ServiceRequestResponse> updateRequestStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest statusUpdate) {
        log.info("REST request to update Service Request ID: {} status to: {}", id, statusUpdate.getStatus());
        ServiceRequestResponse response = serviceRequestService.updateRequestStatus(id, statusUpdate.getStatus());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cancelRequest(@PathVariable Long id) {
        log.info("REST request to cancel/delete Service Request ID: {}", id);
        serviceRequestService.cancelRequest(id);
        return ResponseEntity.noContent().build();
    }
}
