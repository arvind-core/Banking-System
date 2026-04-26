package com.BankingSystem.repo;

import com.BankingSystem.entity.bank.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {

    Optional<Branch> findByBranchCode(String branchCode);

    Optional<Branch> findByIfscCode(String ifscCode);

    List<Branch> findByIsActiveTrue();

    boolean existsByBranchCode(String branchCode);

    boolean existsByIfscCode(String ifscCode);
}