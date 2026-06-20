package com.govtech.gsrp_backend.domain.service.impl;

import com.govtech.gsrp_backend.application.dto.ServiceRequestResponse;
import com.govtech.gsrp_backend.application.dto.ServiceRequestSubmitDTO;
import com.govtech.gsrp_backend.application.exception.BusinessException;
import com.govtech.gsrp_backend.domain.entity.Citizen;
import com.govtech.gsrp_backend.domain.entity.Notification;
import com.govtech.gsrp_backend.domain.entity.ServiceRequest;
import com.govtech.gsrp_backend.domain.entity.User;
import com.govtech.gsrp_backend.domain.enums.NotificationStatus;
import com.govtech.gsrp_backend.domain.enums.RequestStatus;
import com.govtech.gsrp_backend.domain.enums.ServiceType;
import com.govtech.gsrp_backend.domain.service.ServiceRequestExecutionProcessService;
import com.govtech.gsrp_backend.domain.util.Role;
import com.govtech.gsrp_backend.external.repository.CitizenRepository;
import com.govtech.gsrp_backend.external.repository.NotificationRepository;
import com.govtech.gsrp_backend.external.repository.ServiceRequestRepository;
import com.govtech.gsrp_backend.external.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ServiceRequestExecutionProcessServiceImpl implements ServiceRequestExecutionProcessService {

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private CitizenRepository citizenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

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
        log.info("  Service request submitted successfully with ID: {} for Citizen NIC: {}", request.getId(), citizen.getNic());

        // Create initial status change notification
        Notification notification = Notification.builder()
                .citizen(citizen)
                .serviceRequest(request)
                .message("Your Service Request for " + request.getServiceType() + " has been successfully submitted.")
                .status(NotificationStatus.UNREAD)
                .build();
        log.error(" Notification : "+ notification.toString());
        notificationRepository.save(notification);

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
                throw new BusinessException("Access Denied: You cannot view other citizens' requests.");
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
    public ServiceRequestResponse updateRequestStatus(Long id, RequestStatus status) {
        log.info("Updating status for service request ID: {} to status: {}", id, status);
        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Service Request not found with ID: " + id));

        if (request.getStatus() == status) {
            return mapToResponse(request);
        }

        request.setStatus(status);
        request = serviceRequestRepository.save(request);
        log.info("Service request ID {} status updated to {}.", id, status);

        // Generate notification
        Notification notification = Notification.builder()
                .citizen(request.getCitizenReference())
                .serviceRequest(request)
                .message("Your Service Request ID " + request.getId() + " status has been updated to " + status + ".")
                .status(NotificationStatus.UNREAD)
                .build();
        notificationRepository.save(notification);
        log.info("Notification created for Citizen ID {} status update to {}", request.getCitizenReference().getId(), status);

        return mapToResponse(request);
    }

    @Override
    @Transactional
    public void cancelRequest(Long id) {
        log.info("Cancelling service request ID: {}", id);
        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Service Request not found with ID: " + id));

        request.setStatus(RequestStatus.CANCELLED);
        serviceRequestRepository.save(request);
        log.info("Service request ID {} marked as CANCELLED.", id);

        // Generate cancellation notification
        Notification notification = Notification.builder()
                .citizen(request.getCitizenReference())
                .serviceRequest(request)
                .message("Your Service Request ID " + request.getId() + " has been cancelled by the Administrator.")
                .status(NotificationStatus.UNREAD)
                .build();
        notificationRepository.save(notification);
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
}
