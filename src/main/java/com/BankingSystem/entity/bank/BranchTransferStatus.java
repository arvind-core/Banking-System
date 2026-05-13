package com.BankingSystem.entity.bank;

public enum BranchTransferStatus {
    PENDING_CURRENT_BRANCH_APPROVAL,
    PENDING_NEW_BRANCH_APPROVAL,
    APPROVED,
    REJECTED_BY_CURRENT_BRANCH,
    REJECTED_BY_NEW_BRANCH,
    CANCELLED
}