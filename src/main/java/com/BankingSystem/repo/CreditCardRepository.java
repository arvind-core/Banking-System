package com.BankingSystem.repo;

import com.BankingSystem.entity.card.CardStatus;
import com.BankingSystem.entity.card.CardType;
import com.BankingSystem.entity.card.CreditCard;
import com.BankingSystem.entity.users.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CreditCardRepository extends JpaRepository<CreditCard, Long> {

    Optional<CreditCard> findByCardNumber(String cardNumber);

    List<CreditCard> findByUser(User user);

    List<CreditCard> findByUserAndStatus(User user, CardStatus status);

    boolean existsByUserAndCardType(User user, CardType cardType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CreditCard c WHERE c.cardNumber = :cardNumber")
    Optional<CreditCard> findByCardNumberWithLock(@Param("cardNumber") String cardNumber);

    @Query("SELECT c FROM CreditCard c WHERE c.status = 'ACTIVE'")
    List<CreditCard> findAllActiveCards();
}



















