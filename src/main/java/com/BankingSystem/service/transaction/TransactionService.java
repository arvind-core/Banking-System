package com.BankingSystem.service.transaction;

import com.BankingSystem.dto.request.transaction.DepositRequest;
import com.BankingSystem.dto.request.transaction.TransferRequest;
import com.BankingSystem.dto.request.transaction.WithdrawalRequest;
import com.BankingSystem.dto.response.AccountResponse;
import com.BankingSystem.dto.response.AccountStatementResponse;
import com.BankingSystem.dto.response.transaction.BeneficiaryResponse;
import com.BankingSystem.dto.response.transaction.TransactionResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionService {

    TransactionResponse deposit(DepositRequest request);

    TransactionResponse withdraw(WithdrawalRequest request);

    TransactionResponse transfer(TransferRequest request);

    BeneficiaryResponse getBeneficiaryDetails(String identifier, String identifierType);

    List<TransactionResponse> getTransactionHistory(String accountNumber);

    AccountStatementResponse getAccountStatement(String accountNumber, LocalDateTime fromDate, LocalDateTime toDate);

    AccountResponse updatePrimaryAccount(Long userId, String accountNumber);
}