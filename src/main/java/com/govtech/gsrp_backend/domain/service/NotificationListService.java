package com.govtech.gsrp_backend.domain.service;

import com.govtech.gsrp_backend.application.dto.NotificationResponse;

import java.util.List;

public interface NotificationListService {
    List<NotificationResponse> getMyNotifications(String currentUsername);
    NotificationResponse markNotificationAsRead(Long notificationId, String currentUsername);
}
