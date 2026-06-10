package com.BankingSystem.repo;
import com.BankingSystem.entity.bank.Branch;
import com.BankingSystem.entity.loan.LoanApplication;
import com.BankingSystem.entity.loan.LoanStatus;
import com.BankingSystem.entity.loan.LoanType;
import com.BankingSystem.entity.users.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanApplicationRepository
        extends JpaRepository<LoanApplication, Long> {

    Optional<LoanApplication> findByApplicationReference(String reference);

    List<LoanApplication> findByUser(User user);

    List<LoanApplication> findByBranchAndStatus(Branch branch, LoanStatus status);

    List<LoanApplication> findByUserAndLoanType(User user, LoanType loanType);

    long countByUserAndStatusIn(User user, List<LoanStatus> statuses);

    Page<LoanApplication> findByUser(User user, Pageable pageable);

}