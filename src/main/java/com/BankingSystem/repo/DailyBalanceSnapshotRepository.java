package com.BankingSystem.repo;

import com.BankingSystem.entity.account.Account;
import com.BankingSystem.entity.account.DailyBalanceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.Optional;

@Repository
public interface DailyBalanceSnapshotRepository extends JpaRepository<DailyBalanceSnapshot, Long> {

    Optional<DailyBalanceSnapshot> findByAccountAndSnapshotDate(Account account, Date snapshotDate);

    boolean existsByAccountAndSnapshotDate(Account account, Date snapshotDate);

    // Sum all daily balances for an account within a date range
    // This is the core of the daily product method
    @Query("SELECT COALESSCE(SUM(s.closingBalance), 0) FROM DailyBalanceSnapshot s WHERE s.account = :account AND s.snapshotDate >= :startDate AND s.snapshotDate <= :endDate")
    BigDecimal sumBalancesForPeriod(@Param("account") Account account, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
