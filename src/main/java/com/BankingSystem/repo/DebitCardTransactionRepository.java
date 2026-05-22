package com.BankingSystem.repo;

import com.BankingSystem.entity.card.DebitCard;
import com.BankingSystem.entity.card.DebitCardTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DebitCardTransactionRepository extends JpaRepository<DebitCardTransaction, Long> {

    List<DebitCardTransaction> findByDebitCardOrderByCreatedAtDesc(DebitCard debitCard);D
}
