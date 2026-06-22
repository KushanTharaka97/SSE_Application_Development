package com.govtech.gsrp_backend.domain.service.impl;

import com.govtech.gsrp_backend.application.dto.ServiceRequestStatusHistoryResponse;
import com.govtech.gsrp_backend.application.dto.ServiceRequestResponse;
import com.govtech.gsrp_backend.application.dto.ServiceRequestSubmitDTO;
import com.govtech.gsrp_backend.application.exception.BusinessException;
import com.govtech.gsrp_backend.domain.entity.Citizen;
import com.govtech.gsrp_backend.domain.entity.Notification;
import com.govtech.gsrp_backend.domain.entity.ServiceRequest;
import com.govtech.gsrp_backend.domain.entity.ServiceRequestStatusHistory;
import com.govtech.gsrp_backend.domain.entity.User;
import com.govtech.gsrp_backend.domain.enums.NotificationStatus;
import com.govtech.gsrp_backend.domain.enums.RequestStatus;
import com.govtech.gsrp_backend.domain.enums.ServiceType;
import com.govtech.gsrp_backend.domain.service.ServiceRequestExecutionProcessService;
import com.govtech.gsrp_backend.domain.enums.Role;
import com.govtech.gsrp_backend.external.repository.CitizenRepository;
import com.govtech.gsrp_backend.external.repository.NotificationRepository;
import com.govtech.gsrp_backend.external.repository.ServiceRequestRepository;
import com.govtech.gsrp_backend.external.repository.ServiceRequestStatusHistoryRepository;
import com.govtech.gsrp_backend.external.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceRequestExecutionProcessServiceImpl implements ServiceRequestExecutionProcessService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final CitizenRepository citizenRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final ServiceRequestStatusHistoryRepository serviceRequestStatusHistoryRepository;

    @Override
    @Transactional
    public ServiceRequestResponse submitRequest(ServiceRequestSubmitDTO submitDTO, String currentUsername) {
        log.info("Attempting to submit service request. Caller: {}", currentUsername);

        Citizen citizen;
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new BusinessException("Authenticated user details not found."));

        // If submitter is ADMIN/AGENT and specifies citizenId, allow submitting on behalf of that citizen
        boolean isStaff = currentUser.getRoles().contains(Role.ADMIN) || currentUser.getRoles().contains(Role.SERVICE_AGENT);
        if (isStaff && submitDTO.getCitizenId() != null) {
            citizen = citizenRepository.findById(submitDTO.getCitizenId())
                    .orElseThrow(() -> new BusinessException("Citizen profile not found with ID: " + submitDTO.getCitizenId()));
        } else {
            // Find Citizen by authenticated user's username (which is the citizen's NIC)
            citizen = citizenRepository.findByNic(currentUsername)
                    .orElseThrow(() -> new BusinessException("Citizen profile not found for user: " + currentUsername + ". Please contact Administrator."));
        }

        ServiceRequest request = ServiceRequest.builder()
                .citizenReference(citizen)
                .serviceType(submitDTO.getServiceType())
                .description(submitDTO.getDescription())
                .status(RequestStatus.SUBMITTED)
                .build();

        request = serviceRequestRepository.save(request);
        log.info("Service request submitted successfully with ID: {} for Citizen NIC: {}", request.getId(), citizen.getNic());

        createNotification(request, "Your Service Request for " + request.getServiceType() + " has been successfully submitted.");
        createStatusHistory(request, null, RequestStatus.SUBMITTED, currentUsername);

        return mapToResponse(request);
    }

    @Override
    public Page<ServiceRequestResponse> getMyRequests(String currentUsername, int page, int size) {
        log.info("Retrieving service requests for logged-in user: {}", currentUsername);
        Citizen citizen = citizenRepository.findByNic(currentUsername)
                .orElseThrow(() -> new BusinessException("Citizen profile not found."));

        Pageable pageable = PageRequest.of(page, size);
        Page<ServiceRequest> requestPage = serviceRequestRepository.findByCitizenReferenceId(citizen.getId(), pageable);
        return requestPage.map(this::mapToResponse);
    }

    @Override
    public ServiceRequestResponse getRequestById(Long id, String currentUsername) {
        log.info("Fetching service request ID: {}", id);
        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Service Request not found with ID: " + id));

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new BusinessException("User not found"));

        // If the caller is a Citizen, ensure they only view their own service request
        boolean isCitizen = currentUser.getRoles().contains(Role.CITIZEN);
        boolean isStaff = currentUser.getRoles().contains(Role.ADMIN) || currentUser.getRoles().contains(Role.SERVICE_AGENT);

        if (isCitizen && !isStaff) {
            Citizen citizen = citizenRepository.findByNic(currentUsername)
                    .orElseThrow(() -> new BusinessException("Citizen profile not found."));
            if (!request.getCitizenReference().getId().equals(citizen.getId())) {
                log.warn("Access Denied: Citizen {} attempted to view Service Request {} belonging to Citizen {}", 
                        citizen.getId(), id, request.getCitizenReference().getId());
                throw new AccessDeniedException("You cannot view other citizens' requests.");
            }
        }

        return mapToResponse(request);
    }

    @Override
    public Page<ServiceRequestResponse> getAllRequests(Long citizenId, RequestStatus status, ServiceType serviceType, int page, int size) {
        log.info("Service Agent retrieving filtered requests - CitizenID: {}, Status: {}, ServiceType: {}, Page: {}, Size: {}", 
                citizenId, status, serviceType, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<ServiceRequest> requestPage = serviceRequestRepository.findByFilters(citizenId, status, serviceType, pageable);
        return requestPage.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public ServiceRequestResponse updateRequestDetails(Long id, ServiceRequestSubmitDTO submitDTO) {
        log.info("Updating details for service request ID: {}", id);
        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Service Request not found with ID: " + id));

        request.setServiceType(submitDTO.getServiceType());
        request.setDescription(submitDTO.getDescription());

        request = serviceRequestRepository.save(request);
        log.info("Service request ID {} details updated successfully.", id);
        return mapToResponse(request);
    }

    @Override
    @Transactional
    public ServiceRequestResponse updateRequestStatus(Long id, RequestStatus status, String currentUsername) {
        log.info("Updating status for service request ID: {} to status: {} by user: {}", id, status, currentUsername);
        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Service Request not found with ID: " + id));

        RequestStatus previousStatus = request.getStatus();
        if (previousStatus == status) {
            return mapToResponse(request);
        }

        request.setStatus(status);
        request = serviceRequestRepository.save(request);
        log.info("Service request ID {} status updated to {}.", id, status);

        createNotification(request, "Your Service Request ID " + request.getId() + " status has been updated to " + status + ".");
        createStatusHistory(request, previousStatus, status, currentUsername);

        return mapToResponse(request);
    }

    @Override
    @Transactional
    public void cancelRequest(Long id, String currentUsername) {
        log.info("Cancelling service request ID: {} by user: {}", id, currentUsername);
        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Service Request not found with ID: " + id));

        RequestStatus previousStatus = request.getStatus();
        if (previousStatus == RequestStatus.CANCELLED) {
            return;
        }

        request.setStatus(RequestStatus.CANCELLED);
        serviceRequestRepository.save(request);
        log.info("Service request ID {} marked as CANCELLED.", id);

        createNotification(request, "Your Service Request ID " + request.getId() + " has been cancelled by the Administrator.");
        createStatusHistory(request, previousStatus, RequestStatus.CANCELLED, currentUsername);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceRequestStatusHistoryResponse> getStatusHistory(Long id) {
        log.info("Retrieving status history for service request ID: {}", id);

        if (!serviceRequestRepository.existsById(id)) {
            throw new BusinessException("Service Request not found with ID: " + id);
        }

        return serviceRequestStatusHistoryRepository.findByServiceRequestIdOrderByChangedAtAscIdAsc(id)
                .stream()
                .map(this::mapToStatusHistoryResponse)
                .toList();
    }

    private ServiceRequestResponse mapToResponse(ServiceRequest request) {
        return ServiceRequestResponse.builder()
                .id(request.getId())
                .citizenId(request.getCitizenReference().getId())
                .citizenName(request.getCitizenReference().getName())
                .serviceType(request.getServiceType())
                .description(request.getDescription())
                .status(request.getStatus())
                .createdDate(request.getCreatedDate())
                .updatedDate(request.getUpdatedDate())
                .build();
    }

    private ServiceRequestStatusHistoryResponse mapToStatusHistoryResponse(ServiceRequestStatusHistory history) {
        return ServiceRequestStatusHistoryResponse.builder()
                .id(history.getId())
                .serviceRequestId(history.getServiceRequest().getId())
                .previousStatus(history.getPreviousStatus())
                .newStatus(history.getNewStatus())
                .changedBy(history.getChangedBy())
                .changedAt(history.getChangedAt())
                .build();
    }

    private void createNotification(ServiceRequest request, String message) {
        Notification notification = Notification.builder()
                .citizen(request.getCitizenReference())
                .serviceRequest(request)
                .message(message)
                .status(NotificationStatus.UNREAD)
                .build();
        notificationRepository.save(notification);
        log.info("Notification created for Citizen ID {} and service request ID {}", request.getCitizenReference().getId(), request.getId());
    }

    private void createStatusHistory(ServiceRequest request, RequestStatus previousStatus, RequestStatus newStatus, String changedBy) {
        ServiceRequestStatusHistory history = ServiceRequestStatusHistory.builder()
                .serviceRequest(request)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .changedBy(changedBy)
                .build();
        serviceRequestStatusHistoryRepository.save(history);
        log.info("Status history recorded for service request ID {} from {} to {} by {}", request.getId(), previousStatus, newStatus, changedBy);
    }
}
