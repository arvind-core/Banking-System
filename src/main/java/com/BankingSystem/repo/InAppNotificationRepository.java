package com.BankingSystem.repo;

import com.BankingSystem.entity.notification.InAppNotification;
import com.BankingSystem.entity.users.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InAppNotificationRepository extends JpaRepository<InAppNotification, Long> {

    List<InAppNotification> findByRecipientOrderByCreatedAtDesc(User recipient);

    List<InAppNotification> findByRecipientAndIsReadFalseOrderByCreatedAtDesc(User recipient);

    long countByRecipientAndIsReadFalse(User recipient);

    @Modifying
    @Query("UPDATE InAppNotification n SET n.isRead = true WHERE n.recipient = :recipient")
    void markAllAsReadForUser(@Param("recipient") User recipient);

    Page<InAppNotification> findByRecipientOrderByCreatedAtDesc(User recipient, Pageable pageable);

    Page<InAppNotification> findByRecipientAndIsReadFalseOrderByCreatedAtDesc(User recipient, Pageable pageable);
}
