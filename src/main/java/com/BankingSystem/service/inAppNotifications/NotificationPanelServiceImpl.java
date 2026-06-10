package com.BankingSystem.service.inAppNotifications;

import com.BankingSystem.dto.response.PagedResponse;
import com.BankingSystem.dto.response.inAppNotifications.InAppNotificationResponse;
import com.BankingSystem.entity.notification.InAppNotification;
import com.BankingSystem.entity.notification.InAppNotificationType;
import com.BankingSystem.entity.users.User;
import com.BankingSystem.exception.ResourceNotFoundException;
import com.BankingSystem.repo.InAppNotificationRepository;
import com.BankingSystem.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationPanelServiceImpl implements NotificationPanelService{

    private final InAppNotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void sendToUser(Long recipientId, String title, String message, InAppNotificationType type, Long referenceId, String referenceType) {

        User recipient = userRepository.findById(recipientId).orElseThrow(() -> new RuntimeException("Recipient not found: " + recipientId));

        InAppNotification notification = InAppNotification.builder()
                .recipient(recipient)
                .title(title)
                .message(message)
                .type(type)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }

    @Override
    public List<InAppNotificationResponse> getNotifications(Long userId) {

        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Recipient not found: " + userId));

        return notificationRepository
                .findByRecipientOrderByCreatedAtDesc(user)
                .stream()
                .map(this :: mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<InAppNotificationResponse> getUnreadNotifications(Long userId) {

        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Recipient not found: " + userId));

        return notificationRepository
                .findByRecipientAndIsReadFalseOrderByCreatedAtDesc(user)
                .stream()
                .map(this :: mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public long getUnreadCount(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Recipient not found: " + userId));

        return notificationRepository.countByRecipientAndIsReadFalse(user);
    }

    @Override
    @Transactional
    public InAppNotificationResponse markAsRead(Long notificationId) {

        InAppNotification notification = notificationRepository.findById(notificationId).orElseThrow(() ->
                new ResourceNotFoundException("Notification not found : " + notificationId));
        notification.setRead(true);

        return mapToResponse(notificationRepository.save(notification));
    }
    @Override
    @Transactional
    public void markALlAsRead(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Recipient not found: " + userId));

        notificationRepository.markAllAsReadForUser(user);
    }

    private InAppNotificationResponse mapToResponse(InAppNotification n) {

        return InAppNotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .referenceId(n.getReferenceId())
                .referenceType(n.getReferenceType())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }

    @Override
    public PagedResponse<InAppNotificationResponse> getNotificationsPaged(Long userId, int page, int size) {

        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<InAppNotification> notifPage = notificationRepository.findByRecipientOrderByCreatedAtDesc(user, pageable);

        return PagedResponse.<InAppNotificationResponse>builder()
                .content(notifPage.getContent().stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList()))
                .pageNumber(notifPage.getNumber())
                .pageSize(notifPage.getSize())
                .totalElements(notifPage.getTotalElements())
                .totalPages(notifPage.getTotalPages())
                .isLastPage(notifPage.isLast())
                .isFirstPage(notifPage.isFirst())
                .build();
    }
}
