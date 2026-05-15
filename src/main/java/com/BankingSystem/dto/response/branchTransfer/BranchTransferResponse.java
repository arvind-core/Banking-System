package com.BankingSystem.dto.response.branchTransfer;

import com.BankingSystem.entity.bank.BranchTransferStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BranchTransferResponse {
    private Long id;
    private String transferReference;
    private String accountNumber;
    private String accountHolderName;
    private String currentBranchName;
    private String requestedBranchName;
    private String reason;
    private BranchTransferStatus status;
    private String currentBranchComments;
    private String newBranchComments;
    private LocalDateTime currentBranchReviewedAt;
    private LocalDateTime newBranchReviewedAt;
    private LocalDateTime createdAt;
}
