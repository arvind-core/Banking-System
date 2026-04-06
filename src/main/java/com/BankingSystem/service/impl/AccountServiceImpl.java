package com.BankingSystem.service.impl;

import com.BankingSystem.dto.request.CreateAccountRequest;
import com.BankingSystem.dto.response.AccountResponse;
import com.BankingSystem.entity.account.Account;
import com.BankingSystem.entity.users.User;
import com.BankingSystem.repo.AccountRepository;
import com.BankingSystem.repo.UserRepository;
import com.BankingSystem.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Override
    public AccountResponse createAccount(CreateAccountRequest request) {

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUserId()));

        String accountNumber = generateUniqueAccountNumber();

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .accountType(request.getAccountType())
                .balance(BigDecimal.ZERO)
                .branchName(request.getBranchName())
                .user(user)
                .build();

        Account savedAccount = accountRepository.save(account);

        return mapToAccountResponse(savedAccount);
    }

    @Override
    public AccountResponse getAccountByAccountNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found with account number: " + accountNumber));

        return mapToAccountResponse(account);
    }

    @Override
    public List<AccountResponse> getAccountsByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        return accountRepository.findByUser(user)
                .stream()
                .map(this::mapToAccountResponse)
                .collect(Collectors.toList());
    }

    private String generateUniqueAccountNumber() {
        Random random = new Random();
        String accountNumber;

        do {
            long number = (long) (random.nextDouble() * 900000000000L) + 100000000000L;
            accountNumber = String.valueOf(number);
        } while (accountRepository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }

    private AccountResponse mapToAccountResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .branchName(account.getBranchName())
                .ownerFirstName(account.getUser().getFirstName())
                .ownerLastName(account.getUser().getLastName())
                .createdAt(account.getCreatedAt())
                .build();
    }
}