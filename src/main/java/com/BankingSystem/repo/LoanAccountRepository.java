package com.BankingSystem.repo;

import com.BankingSystem.entity.bank.Branch;
import com.BankingSystem.entity.loan.LoanAccount;
import com.BankingSystem.entity.loan.LoanStatus;
import com.BankingSystem.entity.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanAccountRepository
        extends JpaRepository<LoanAccount, Long> {

    Optional<LoanAccount> findByLoanAccountNumber(String loanAccountNumber);

    List<LoanAccount> findByUser(User user);

    List<LoanAccount> findByUserAndStatus(User user, LoanStatus status);

    List<LoanAccount> findByCurrentBranchAndStatus(Branch branch, LoanStatus status);

    long countByCurrentBranchAndStatus(Branch branch, LoanStatus status);

    @Query("SELECT COALSECE(SUM(la.outstandingPrincipal),0) FROM LoanAccount la WHERE la.currentBranch = :branch AND la.status = 'ACTIVE'")
    BigDecimal sumActiveLoanBookByBranch(@Param("branch") Branch branch);

    @Query("SELECT la FROM LoanAccount la WHERE la.status = 'ACTIVE'")
    List<LoanAccount> findAllActiveLoans();
}