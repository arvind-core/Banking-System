package com.BankingSystem.repo;

import com.BankingSystem.entity.loan.EMISchedule;
import com.BankingSystem.entity.loan.EMIStatus;
import com.BankingSystem.entity.loan.LoanAccount;
import org.springframework.cglib.core.Local;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface EMIScheduleRepository
        extends JpaRepository<EMISchedule, Long> {

    List<EMISchedule> findByLoanAccountOrderByInstallmentNumberAsc(
            LoanAccount loanAccount);

    List<EMISchedule> findByLoanAccountAndStatus(LoanAccount loanAccount, EMIStatus status);

    @Query("SELECT e FROM EMISchedule e WHERE e.dueDate = :today " +
           "AND e.status = 'PENDING'")
    List<EMISchedule> findEmisDueToday(@Param("today") LocalDate today);

    @Query("SELECT e FROM EMISchedule e WHERE e.dueDate = :reminderDate " +
           "AND e.status = 'PENDING'")
    List<EMISchedule> findEmisForReminder(@Param("reminderDate") LocalDate reminderDate);

    @Query("SELECT e FROM EMISchedule e WHERE e.status = 'PENDING' AND e.dueDate < :today AND e.retryCount < :maxRetries")
    List<EMISchedule> findEmisForRetry(@Param("today") LocalDate today, @Param("maxRetries") int maxRetries);

    @Query("SELECT e FROM EMISchedule e WHERE e.status = 'PENDING' AND e.dueDate <= :graceDeadline AND e.retryCount >= :maxRetries")
    List<EMISchedule> findEmisExhaustedRetries(@Param("graceDeadline")LocalDate graceDeadline, @Param("maxRetries") int  maxRetries);
}