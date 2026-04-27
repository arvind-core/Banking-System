package com.BankingSystem.repo;

import com.BankingSystem.entity.loan.LoanAccount;
import com.BankingSystem.entity.loan.LoanStatus;
import com.BankingSystem.entity.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanAccountRepository
        extends JpaRepository<LoanAccount, Long> {

    Optional<LoanAccount> findByLoanAccountNumber(String loanAccountNumber);

    List<LoanAccount> findByUser(User user);

    List<LoanAccount> findByUserAndStatus(User user, LoanStatus status);

    @Query("SELECT la FROM LoanAccount la WHERE la.status = 'ACTIVE'")
    List<LoanAccount> findAllActiveLoans();
}