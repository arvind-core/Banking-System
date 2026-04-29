package com.BankingSystem.repo;

import com.BankingSystem.entity.bank.BankLedger;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankLedgerRepository extends JpaRepository<BankLedger, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BankLedger b WHERE b.id = 1")
    Optional<BankLedger> findLedgerWithLock();

    @Query("SELECT b FROM BankLedger b WHERE b.id = 1")
    Optional<BankLedger> findLedger();

}
