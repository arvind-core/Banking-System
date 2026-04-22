package com.BankingSystem.service.user.impl;

import com.BankingSystem.dto.request.CreateAccountRequest;
import com.BankingSystem.dto.response.AccountResponse;
import com.BankingSystem.entity.account.Account;
import com.BankingSystem.entity.users.User;
import com.BankingSystem.exception.InvalidOperationException;
import com.BankingSystem.exception.ResourceNotFoundException;
import com.BankingSystem.repo.AccountRepository;
import com.BankingSystem.repo.UserRepository;
import com.BankingSystem.service.user.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .orElseThrow(() ->  new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        String accountNumber = generateUniqueAccountNumber();

        List<Account> existingAccounts = accountRepository.findByUserAndIsActiveTrue(user);
        boolean isFirstAccount = existingAccounts.isEmpty();

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .accountType(request.getAccountType())
                .balance(BigDecimal.ZERO)
                .branchName(request.getBranchName())
                .isPrimary(isFirstAccount)
                .user(user)
                .build();

        Account savedAccount = accountRepository.save(account);

        return mapToAccountResponse(savedAccount);
    }

    @Override
    public AccountResponse getAccountByAccountNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumberAndIsActiveTrue(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));

        return mapToAccountResponse(account);
    }

    @Override
    public List<AccountResponse> getAccountsByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return accountRepository.findByUserAndIsActiveTrue(user)
                .stream()
                .map(this::mapToAccountResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AccountResponse closeAccount(String accountNumber) {

        Account account = accountRepository
                .findByAccountNumberAndIsActiveTrue(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found: " + accountNumber));

        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new InvalidOperationException(
                    "Cannot close account with positive balance. Please withdraw funds first.");
        }

        if (account.isPrimary()) {
            throw new InvalidOperationException(
                    "Cannot close primary account. Please set another account as primary first.");
        }

        account.setActive(false);
        Account saved = accountRepository.save(account);

        return AccountResponse.builder()
                .id(saved.getId())
                .accountNumber(saved.getAccountNumber())
                .accountType(saved.getAccountType())
                .balance(saved.getBalance())
                .branchName(saved.getBranchName())
                .ownerFirstName(saved.getUser().getFirstName())
                .ownerLastName(saved.getUser().getLastName())
                .createdAt(saved.getCreatedAt())
                .build();
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