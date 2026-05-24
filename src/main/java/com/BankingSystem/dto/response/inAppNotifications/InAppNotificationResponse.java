package com.BankingSystem.dto.response.inAppNotifications;

import com.BankingSystem.entity.notification.InAppNotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InAppNotificationResponse {

    private Long id;
    private String title;
    private String message;
    private InAppNotificationType type;
    private Long referenceId;
    private String referenceType;
    private boolean isRead;
    private LocalDateTime createdAt;
}
