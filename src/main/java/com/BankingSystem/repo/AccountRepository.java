package com.BankingSystem.repo;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.BankingSystem.entity.account.Account;
import com.BankingSystem.entity.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);
    
    List<Account> findByUser(User user);
    
    Boolean existsByAccountNumber(String accountNumber);

    List<Account> findByUserAndIsActiveTrue(User user);

    Optional<Account> findByAccountNumberAndIsActiveTrue(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber AND a.isActive = true")
    Optional<Account> findByAccountNumberWithLock(@Param("accountNumber") String accountNumber);

    @Query("SELECT a FROM Account a WHERE a.user.phoneNumber = :phoneNumber AND a.isPrimary = true AND a.isActive = true")
    Optional<Account> findPrimaryAccountByUserPhoneNumber(@Param("phoneNumber") String phoneNumber);
}