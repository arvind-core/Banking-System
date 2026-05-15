package com.BankingSystem.dto.request.branchTransfer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BranchTransferCreate {

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotBlank(message = "Requested branch code is required")
    private String requestedBranchCode;

    @NotBlank(message = "Reason is required")
    private String reason;
}

