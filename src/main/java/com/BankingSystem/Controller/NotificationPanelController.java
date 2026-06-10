package com.BankingSystem.Controller;

import com.BankingSystem.dto.response.PagedResponse;
import com.BankingSystem.dto.response.inAppNotifications.InAppNotificationResponse;
import com.BankingSystem.service.inAppNotifications.NotificationPanelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationPanelController {

    private final NotificationPanelService notificationPanelService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<InAppNotificationResponse>>
            getNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(
                notificationPanelService.getNotifications(userId));
    }

    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<InAppNotificationResponse>>
            getUnreadNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(
                notificationPanelService.getUnreadNotifications(userId));
    }

    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Long> getUnreadCount(
            @PathVariable Long userId) {
        return ResponseEntity.ok(
                notificationPanelService.getUnreadCount(userId));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<InAppNotificationResponse> markAsRead(
            @PathVariable Long notificationId) {
        return ResponseEntity.ok(
                notificationPanelService.markAsRead(notificationId));
    }

    @PatchMapping("/user/{userId}/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @PathVariable Long userId) {
        notificationPanelService.markALlAsRead(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{userId}/paged")
    public ResponseEntity<PagedResponse<InAppNotificationResponse>> getNotificationsPaged(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(
                notificationPanelService.getNotificationsPaged(userId, page, size));
    }
}