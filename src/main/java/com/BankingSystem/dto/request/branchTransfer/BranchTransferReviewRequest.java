package com.BankingSystem.dto.request.branchTransfer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BranchTransferReviewRequest {

    @NotBlank(message = "Transfer reference is required")
    private String transferReference;

    @NotNull(message = "Manager user ID is required")
    private Long managerUserId;

    @NotNull(message = "Decision is required")
    private Boolean approved;

    private String comments;
}
