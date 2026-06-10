package com.BankingSystem.service.inAppNotifications;

import com.BankingSystem.dto.response.PagedResponse;
import com.BankingSystem.dto.response.inAppNotifications.InAppNotificationResponse;
import com.BankingSystem.entity.notification.InAppNotificationType;

import java.util.List;

public interface NotificationPanelService {

    void sendToUser(Long recipientId, String title, String message, InAppNotificationType type, Long referenceId, String referenceType);

    List<InAppNotificationResponse> getNotifications(Long userId);

    List<InAppNotificationResponse> getUnreadNotifications(Long userId);

    long getUnreadCount(Long userId);

    InAppNotificationResponse markAsRead(Long notificationId);

    void markALlAsRead(Long userId);

    PagedResponse<InAppNotificationResponse> getNotificationsPaged(Long userId, int page, int size);
}
