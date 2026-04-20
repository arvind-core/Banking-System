package com.BankingSystem.dto.response;

import com.BankingSystem.dto.response.transaction.TransactionResponse;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AccountStatementResponse {

    private String accountNumber;
    private String accountHolderName;
    private BigDecimal currentBalance;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private BigDecimal totalDeposits;
    private BigDecimal totalWithdrawals;
    private BigDecimal netFlow;
    private List<TransactionResponse> transactions;
}