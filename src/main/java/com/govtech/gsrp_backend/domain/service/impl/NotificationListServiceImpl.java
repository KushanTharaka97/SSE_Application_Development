package com.govtech.gsrp_backend.domain.service.impl;

import com.govtech.gsrp_backend.application.dto.NotificationResponse;
import com.govtech.gsrp_backend.application.exception.BusinessException;
import com.govtech.gsrp_backend.domain.entity.Citizen;
import com.govtech.gsrp_backend.domain.entity.Notification;
import com.govtech.gsrp_backend.domain.enums.NotificationStatus;
import com.govtech.gsrp_backend.domain.service.NotificationListService;
import com.govtech.gsrp_backend.external.repository.CitizenRepository;
import com.govtech.gsrp_backend.external.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class NotificationListServiceImpl implements NotificationListService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private CitizenRepository citizenRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(String currentUsername) {
        log.info("Retrieving notifications for citizen user: {}", currentUsername);

        Citizen citizen = citizenRepository.findByNic(currentUsername)
                .orElseThrow(() -> new BusinessException("Citizen profile not found for user: " + currentUsername));

        return notificationRepository.findByCitizenIdOrderByCreatedDateDesc(citizen.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public NotificationResponse markNotificationAsRead(Long notificationId, String currentUsername) {
        log.info("Marking notification ID: {} as read for citizen user: {}", notificationId, currentUsername);

        Citizen citizen = citizenRepository.findByNic(currentUsername)
                .orElseThrow(() -> new BusinessException("Citizen profile not found for user: " + currentUsername));

        Notification notification = notificationRepository.findByIdAndCitizenId(notificationId, citizen.getId())
                .orElseThrow(() -> new BusinessException("Notification not found for ID: " + notificationId));

        if (notification.getStatus() == NotificationStatus.READ) {
            return mapToResponse(notification);
        }

        notification.setStatus(NotificationStatus.READ);
        notification = notificationRepository.save(notification);
        log.info("Notification ID: {} marked as READ for citizen ID: {}", notificationId, citizen.getId());

        return mapToResponse(notification);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .citizenId(notification.getCitizen().getId())
                .serviceRequestId(notification.getServiceRequest().getId())
                .serviceType(notification.getServiceRequest().getServiceType())
                .serviceRequestStatus(notification.getServiceRequest().getStatus())
                .message(notification.getMessage())
                .status(notification.getStatus())
                .createdDate(notification.getCreatedDate())
                .build();
    }
}
