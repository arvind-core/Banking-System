package com.BankingSystem.repo;

import com.BankingSystem.entity.bank.Branch;
import com.BankingSystem.entity.bank.BranchTransferRequest;
import com.BankingSystem.entity.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BranchTransferRequestRepository extends JpaRepository<BranchTransferRequestRepository, Long> {

    Optional<BranchTransferRequest> findByTransferReference(String transferReference);

    List<BranchTransferRequest> findByUser(User user);

    // Requests waiting for current branch manager approval
    @Query("SELECT r FROM BranchTransferRequest r WHERE r.currentBranch = :branch AND r.status = 'PENDING_CURRENT_BRANCH_APPROVAL'")
    List<BranchTransferRequest> findPendingForCurrentBranch(@Param("branch") Branch branch);

    // Request waiting for new branch manager approval
    @Query("SELECT r FROM BranchTransferRequest r WHERE r.requestedBranch = :branch AND r.status = 'PENDING_NEW_BRANCH_APPROVAL'")
    List<BranchTransferRequest> findPendingForNewBranch(@Param("branch") Branch branch);

    // Check if account has active pending transfer
    @Query("SELECT COUNT(r) > 0 FROM BranchTransferRequest r WHERE r.account.accountNumber = :accountNumber AND r.status IN ('PENDING_CURRENT_BRANCH_APPROVAL','PENDING_NEW_BRANCH_APPROVAL')")
    boolean hasActivePendingTransfer(@Param("accountNumber") String accountNumber);
}
