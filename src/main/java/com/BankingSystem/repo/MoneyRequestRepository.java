package com.BankingSystem.repo;

import com.BankingSystem.entity.requests.MoneyRequest;
import com.BankingSystem.entity.requests.MoneyRequestStatus;
import com.BankingSystem.entity.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MoneyRequestRepository extends JpaRepository<MoneyRequest, Long> {

    Optional<MoneyRequest> findByRequestReference(String reference);

    List<MoneyRequest> findByPayerAndStatusOrderByCreatedAtDesc(User payer, MoneyRequestStatus status);

    List<MoneyRequest> findByRequesterOrderByCreatedAtDesc(User requester);

    List<MoneyRequest> findByPayerOrderByCreatedAtDesc(User payer);

    @Query("SELECT m FROM MoneyRequest m WHERE m.status = 'PENDING' AND m.expiresAt < :now")
    List<MoneyRequest> findExpiredRequests(@Param("now") LocalDateTime now);
}
