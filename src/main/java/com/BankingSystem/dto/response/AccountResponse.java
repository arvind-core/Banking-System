package com.BankingSystem.dto.response;

import com.BankingSystem.entity.account.AccountType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AccountResponse {

    private Long id;
    private String accountNumber;
    private AccountType accountType;
    private BigDecimal balance;
    private String branchName;
    private String ownerFirstName;
    private String ownerLastName;
    private LocalDateTime createdAt;
}