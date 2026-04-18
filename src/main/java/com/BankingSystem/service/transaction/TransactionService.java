package com.BankingSystem.service.transaction;

import com.BankingSystem.dto.request.transaction.DepositRequest;
import com.BankingSystem.dto.request.transaction.TransferRequest;
import com.BankingSystem.dto.request.transaction.WithdrawalRequest;
import com.BankingSystem.dto.response.transaction.BeneficiaryResponse;
import com.BankingSystem.dto.response.transaction.TransactionResponse;
import java.util.List;

public interface TransactionService {

    TransactionResponse deposit(DepositRequest request);

    TransactionResponse withdraw(WithdrawalRequest request);

    TransactionResponse transfer(TransferRequest request);

    BeneficiaryResponse getBeneficiaryDetails(String identifier, String identifierType);

    List<TransactionResponse> getTransactionHistory(String accountNumber);
}