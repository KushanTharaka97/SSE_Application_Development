package com.govtech.gsrp_backend.application.controller;

import com.govtech.gsrp_backend.application.dto.ApiResponseFactory;
import com.govtech.gsrp_backend.application.dto.ApiSuccessResponse;
import com.govtech.gsrp_backend.application.dto.NotificationResponse;
import com.govtech.gsrp_backend.domain.service.NotificationListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationListService notificationListService;

    @GetMapping("/my")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<ApiSuccessResponse<List<NotificationResponse>>> getMyNotifications(Principal principal) {
        log.info("REST request to get notifications for caller: {}", principal.getName());
        List<NotificationResponse> response = notificationListService.getMyNotifications(principal.getName());
        return ApiResponseFactory.ok("Notifications retrieved successfully.", response);
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<ApiSuccessResponse<NotificationResponse>> markNotificationAsRead(
            @PathVariable Long id,
            Principal principal) {
        log.info("REST request to mark notification ID: {} as read for caller: {}", id, principal.getName());
        NotificationResponse response = notificationListService.markNotificationAsRead(id, principal.getName());
        return ApiResponseFactory.ok("Notification marked as read successfully.", response);
    }
}
