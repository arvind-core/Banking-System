package com.BankingSystem.dto.response.transaction;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BeneficiaryResponse {

    private String accountNumber;
    private String accountHolderFirstName;
    private String accountHolderLastName;
    private String bankBranch;
}