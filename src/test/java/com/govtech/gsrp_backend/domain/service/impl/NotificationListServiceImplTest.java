package com.govtech.gsrp_backend.domain.service.impl;

import com.govtech.gsrp_backend.application.dto.NotificationResponse;
import com.govtech.gsrp_backend.application.exception.BusinessException;
import com.govtech.gsrp_backend.domain.entity.Citizen;
import com.govtech.gsrp_backend.domain.entity.Notification;
import com.govtech.gsrp_backend.domain.entity.ServiceRequest;
import com.govtech.gsrp_backend.domain.entity.User;
import com.govtech.gsrp_backend.domain.enums.NotificationStatus;
import com.govtech.gsrp_backend.domain.enums.RequestStatus;
import com.govtech.gsrp_backend.domain.enums.Role;
import com.govtech.gsrp_backend.domain.enums.ServiceType;
import com.govtech.gsrp_backend.external.repository.CitizenRepository;
import com.govtech.gsrp_backend.external.repository.NotificationRepository;
import com.govtech.gsrp_backend.external.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationListServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private CitizenRepository citizenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationListServiceImpl notificationListService;

    @Test
    void getMyNotifications_returnsMappedNotificationsForCitizen() {
        Citizen citizen = Citizen.builder().id(1L).nic("971234567V").name("Citizen One").build();
        ServiceRequest request = ServiceRequest.builder()
                .id(10L)
                .citizenReference(citizen)
                .serviceType(ServiceType.DOCUMENT_RENEWAL)
                .status(RequestStatus.IN_REVIEW)
                .description("desc")
                .build();
        Notification notification = Notification.builder()
                .id(100L)
                .citizen(citizen)
                .serviceRequest(request)
                .message("Status updated")
                .status(NotificationStatus.UNREAD)
                .createdDate(LocalDateTime.of(2026, 6, 21, 10, 0))
                .build();
        User user = User.builder()
                .username("971234567V")
                .email("citizen@example.com")
                .roles(Collections.singleton(Role.CITIZEN))
                .build();

        when(userRepository.findByUsername("971234567V")).thenReturn(Optional.of(user));
        when(citizenRepository.findByNic("971234567V")).thenReturn(Optional.of(citizen));
        when(notificationRepository.findByCitizenIdOrderByCreatedDateDesc(1L)).thenReturn(List.of(notification));

        List<NotificationResponse> response = notificationListService.getMyNotifications("971234567V");

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getId()).isEqualTo(100L);
        assertThat(response.getFirst().getServiceRequestId()).isEqualTo(10L);
        assertThat(response.getFirst().getStatus()).isEqualTo(NotificationStatus.UNREAD);
    }

    @Test
    void markNotificationAsRead_updatesUnreadNotification() {
        Citizen citizen = Citizen.builder().id(1L).nic("971234567V").build();
        ServiceRequest request = ServiceRequest.builder()
                .id(10L)
                .citizenReference(citizen)
                .serviceType(ServiceType.DOCUMENT_RENEWAL)
                .status(RequestStatus.APPROVED)
                .description("desc")
                .build();
        Notification notification = Notification.builder()
                .id(200L)
                .citizen(citizen)
                .serviceRequest(request)
                .message("Approved")
                .status(NotificationStatus.UNREAD)
                .createdDate(LocalDateTime.of(2026, 6, 21, 11, 0))
                .build();
        User user = User.builder()
                .username("971234567V")
                .email("citizen@example.com")
                .roles(Collections.singleton(Role.CITIZEN))
                .build();

        when(userRepository.findByUsername("971234567V")).thenReturn(Optional.of(user));
        when(citizenRepository.findByNic("971234567V")).thenReturn(Optional.of(citizen));
        when(notificationRepository.findByIdAndCitizenId(200L, 1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        NotificationResponse response = notificationListService.markNotificationAsRead(200L, "971234567V");

        assertThat(response.getStatus()).isEqualTo(NotificationStatus.READ);
        verify(notificationRepository).save(notification);
    }

    @Test
    void markNotificationAsRead_throwsWhenCitizenProfileMissing() {
        User user = User.builder()
                .username("missing")
                .email("missing@example.com")
                .roles(Collections.singleton(Role.CITIZEN))
                .build();

        when(userRepository.findByUsername("missing")).thenReturn(Optional.of(user));
        when(citizenRepository.findByNic("missing")).thenReturn(Optional.empty());
        when(citizenRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationListService.markNotificationAsRead(1L, "missing"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Citizen profile not found");
    }
}
