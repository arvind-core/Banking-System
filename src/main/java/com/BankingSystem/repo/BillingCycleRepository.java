package com.BankingSystem.repo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.BankingSystem.entity.card.BillingCycle;
import com.BankingSystem.entity.card.BillingStatus;
import com.BankingSystem.entity.card.CreditCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillingCycleRepository extends JpaRepository<BillingCycle, Long> {

    List<BillingCycle> findByCreditCardOrderByCycleStartDateDesc(CreditCard creditCard);

    Optional<BillingCycle> findByCreditCardAndStatus(CreditCard creditCard, BillingStatus status);

    @Query("SELECT b FROM BillingCycle b WHERE b.status = 'OPEN'")
    List<BillingCycle> findAllOpenCycle();

    @Query("SELECT b FROM BillingCycle b WHERE b.paymentDueDate < :today AND b.status IN ('GENERATED' , 'PARTIALLY_PAID')")
    List<BillingCycle> findOverdueCycles(@Param("today") LocalDate today);

    boolean existsByCreditCardAndCycleStartDateAndStatus(
            CreditCard creditCard,
            LocalDate cycleStartDate,
            BillingStatus status);
}
