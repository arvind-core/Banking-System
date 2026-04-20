package com.BankingSystem.repo;

import java.math.BigDecimal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.BankingSystem.entity.account.Account;
import com.BankingSystem.entity.transactions.Transaction;
import com.BankingSystem.entity.transactions.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionReference(String transactionReference);

    List<Transaction> findByAccountOrderByCreatedAtDesc(Account account);

    List<Transaction> findByAccountAndTransactionTypeOrderByCreatedAtDesc(
            Account account,
            TransactionType transactionType
    );

    List<Transaction> findByAccountAndCreatedAtBetweenOrderByCreatedAtDesc(
            Account account,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.account = :account " +
            "AND t.transactionType = :type " +
            "AND t.status = 'SUCCESS' " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumByAccountAndTypeAndDateRange(
            @Param("account") Account account,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}