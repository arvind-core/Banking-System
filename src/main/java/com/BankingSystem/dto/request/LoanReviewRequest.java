package com.BankingSystem.dto.request;

import com.BankingSystem.entity.loan.LoanStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoanReviewRequest {

    @NotNull(message = "application reference is required")
    private String applicationReference;

    @NotNull(message = "decision is required")
    private LoanStatus decision;

    private String comments;

    private long reviewByUserId;

}
