package com.govtech.gsrp_backend.application.controller;

import com.govtech.gsrp_backend.application.dto.NotificationResponse;
import com.govtech.gsrp_backend.domain.service.NotificationListService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationListService notificationListService;

    @GetMapping("/my")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(Principal principal) {
        log.info("REST request to get notifications for caller: {}", principal.getName());
        List<NotificationResponse> response = notificationListService.getMyNotifications(principal.getName());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<NotificationResponse> markNotificationAsRead(
            @PathVariable Long id,
            Principal principal) {
        log.info("REST request to mark notification ID: {} as read for caller: {}", id, principal.getName());
        NotificationResponse response = notificationListService.markNotificationAsRead(id, principal.getName());
        return ResponseEntity.ok(response);
    }
}
