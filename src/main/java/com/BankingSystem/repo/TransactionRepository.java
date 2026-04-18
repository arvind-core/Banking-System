package com.BankingSystem.repo;

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
}