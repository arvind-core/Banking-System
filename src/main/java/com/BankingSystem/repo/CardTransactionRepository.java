package com.BankingSystem.repo;

import com.BankingSystem.entity.card.CardTransaction;
import com.BankingSystem.entity.card.CreditCard;
import com.BankingSystem.entity.card.SpendCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CardTransactionRepository
        extends JpaRepository<CardTransaction, Long> {

    List<CardTransaction> findByCreditCardOrderByCreatedAtDesc(
            CreditCard creditCard);

    List<CardTransaction> findByCreditCardAndCreatedAtBetween(
            CreditCard creditCard,
            LocalDateTime from,
            LocalDateTime to);

    @Query("SELECT SUM(ct.amount) FROM CardTransaction ct " +
            "WHERE ct.creditCard = :card " +
            "AND ct.category = :category " +
            "AND ct.createdAt BETWEEN :from AND :to")
    java.math.BigDecimal sumByCardAndCategoryAndDateRange(
            @Param("card") CreditCard card,
            @Param("category") SpendCategory category,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}