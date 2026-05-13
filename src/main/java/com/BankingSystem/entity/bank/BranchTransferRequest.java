package com.BankingSystem.entity.bank;

import com.BankingSystem.entity.account.Account;
import com.BankingSystem.entity.users.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "branch_transfer_requests",
    indexes = {
        @Index(name = "idx_transfer_account", columnList = "account_id"),
        @Index(name = "idx_transfer_status", columnList = "status")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchTransferRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String transferReference;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "current_branch_id", nullable = false)
    private Branch currentBranch;

    @ManyToOne
    @JoinColumn(name = "requested_branch_id", nullable = false)
    private Branch requestedBranch;

    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BranchTransferStatus status;

    // Current branch manager response
    @ManyToOne
    @JoinColumn(name = "current_branch_reviewed_by")
    private User currentBranchReviewedBy;

    @Column
    private String currentBranchComments;

    @Column
    private LocalDateTime currentBranchReviewedAt;

    // New branch manager response
    @ManyToOne
    @JoinColumn(name = "new_branch_reviewed_by")
    private User newBranchReviewedBy;

    @Column
    private String newBranchComments;

    @Column
    private LocalDateTime newBranchReviewedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}