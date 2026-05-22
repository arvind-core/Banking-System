package com.BankingSystem.repo;

import com.BankingSystem.entity.account.Account;
import com.BankingSystem.entity.card.DebitCard;
import com.BankingSystem.entity.card.DebitCardType;
import com.BankingSystem.entity.users.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DebitCardRepository extends JpaRepository<DebitCard, Long> {

    Optional<DebitCard> findByCardNumber(String cardNumber);

    Optional<DebitCard> findByAccount(Account account);

    List<DebitCard> findByUser(User user);

    boolean existsByAccount(Account account);

    boolean existsByAccountAndCardType(Account account, DebitCardType cardType);

    long countByAccount(Account account);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DebitCard d WHERE d.cardNumber = :cardNumber")
    Optional<DebitCard> findByCardNumberWithLock(@Param("cardNumber") String cardNumber);
}
