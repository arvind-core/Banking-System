package com.BankingSystem.service.user;

import com.BankingSystem.dto.request.CreateAccountRequest;
import com.BankingSystem.dto.response.AccountResponse;
import java.util.List;

public interface AccountService {

    AccountResponse createAccount(CreateAccountRequest request);
    
    AccountResponse getAccountByAccountNumber(String accountNumber);
    
    List<AccountResponse> getAccountsByUser(Long userId);

    AccountResponse closeAccount(String accountNumber);
}