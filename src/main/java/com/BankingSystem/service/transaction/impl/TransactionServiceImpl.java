package com.BankingSystem.service.transaction.impl;

import com.BankingSystem.dto.request.transaction.DepositRequest;
import com.BankingSystem.dto.request.transaction.TransferRequest;
import com.BankingSystem.dto.request.transaction.WithdrawalRequest;
import com.BankingSystem.dto.response.AccountResponse;
import com.BankingSystem.dto.response.AccountStatementResponse;
import com.BankingSystem.dto.response.transaction.BeneficiaryResponse;
import com.BankingSystem.dto.response.transaction.TransactionResponse;
import com.BankingSystem.entity.account.Account;
import com.BankingSystem.entity.transactions.Transaction;
import com.BankingSystem.entity.transactions.TransactionStatus;
import com.BankingSystem.entity.transactions.TransactionType;
import com.BankingSystem.entity.users.User;
import com.BankingSystem.exception.InsufficientBalanceException;
import com.BankingSystem.exception.InvalidOperationException;
import com.BankingSystem.exception.ResourceNotFoundException;
import com.BankingSystem.repo.AccountRepository;
import com.BankingSystem.repo.TransactionRepository;
import com.BankingSystem.repo.UserRepository;
import com.BankingSystem.service.transaction.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public TransactionResponse deposit(DepositRequest request) {

        Account account = accountRepository
                .findByAccountNumberWithLock(request.getAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + request.getAccountNumber()));

        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .transactionReference(generateReference())
                .transactionType(TransactionType.DEPOSIT)
                .status(TransactionStatus.SUCCESS)
                .amount(request.getAmount())
                .balanceAfterTransaction(account.getBalance())
                .account(account)
                .description(request.getDescription() != null
                        ? request.getDescription()
                        : "Deposit")
                .build();

        Transaction saved = transactionRepository.save(transaction);
        return mapToTransactionResponse(saved);
    }

    @Override
    @Transactional
    public TransactionResponse withdraw(WithdrawalRequest request) {

        Account account = accountRepository
                .findByAccountNumberWithLock(request.getAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + request.getAccountNumber()));

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .transactionReference(generateReference())
                .transactionType(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.SUCCESS)
                .amount(request.getAmount())
                .balanceAfterTransaction(account.getBalance())
                .account(account)
                .description(request.getDescription() != null
                        ? request.getDescription()
                        : "Withdrawal")
                .build();

        Transaction saved = transactionRepository.save(transaction);
        return mapToTransactionResponse(saved);
    }

    @Override
    @Transactional
    public TransactionResponse transfer(TransferRequest request) {

        Account senderAccount = accountRepository
                .findByAccountNumberWithLock(request.getSenderAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Sender account not found"));

        Account receiverAccount;

        if (request.getTransferType() == TransferRequest.TransferType.PHONE_NUMBER) {
            receiverAccount = accountRepository
                    .findPrimaryAccountByUserPhoneNumber(request.getReceiverIdentifier())
                    .orElseThrow(() -> new ResourceNotFoundException("No primary account found for phone number: " + request.getReceiverIdentifier()));
        }
        else {
            receiverAccount = accountRepository
                    .findByAccountNumberWithLock(request.getReceiverIdentifier())
                    .orElseThrow(() ->  new ResourceNotFoundException("Receiver account not found"));
        }

        if (senderAccount.getAccountNumber()
                .equals(receiverAccount.getAccountNumber())) {
            throw new InvalidOperationException("Cannot transfer to the same account");
        }

        if (senderAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        senderAccount.setBalance(
                senderAccount.getBalance().subtract(request.getAmount()));
        receiverAccount.setBalance(
                receiverAccount.getBalance().add(request.getAmount()));

        accountRepository.save(senderAccount);
        accountRepository.save(receiverAccount);

        String reference = generateReference();
        String description = request.getDescription() != null
                ? request.getDescription()
                : "Transfer";

        Transaction debitTransaction = Transaction.builder()
                .transactionReference(reference + "-DR")
                .transactionType(TransactionType.TRANSFER_DEBIT)
                .status(TransactionStatus.SUCCESS)
                .amount(request.getAmount())
                .balanceAfterTransaction(senderAccount.getBalance())
                .account(senderAccount)
                .targetAccountNumber(receiverAccount.getAccountNumber())
                .description(description)
                .build();

        Transaction creditTransaction = Transaction.builder()
                .transactionReference(reference + "-CR")
                .transactionType(TransactionType.TRANSFER_CREDIT)
                .status(TransactionStatus.SUCCESS)
                .amount(request.getAmount())
                .balanceAfterTransaction(receiverAccount.getBalance())
                .account(receiverAccount)
                .targetAccountNumber(senderAccount.getAccountNumber())
                .description(description)
                .build();

        transactionRepository.save(debitTransaction);
        transactionRepository.save(creditTransaction);

        return mapToTransactionResponse(debitTransaction);
    }

    @Override
    public BeneficiaryResponse getBeneficiaryDetails(
            String identifier, String identifierType) {

        Account account;

        if (identifierType.equalsIgnoreCase("PHONE_NUMBER")) {
            account = accountRepository
                    .findPrimaryAccountByUserPhoneNumber(identifier)
                    .orElseThrow(() -> new ResourceNotFoundException("No account found for phone number: " + identifier));
        }
        else {
            account = accountRepository
                    .findByAccountNumber(identifier)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + identifier));
        }

        return BeneficiaryResponse.builder()
                .accountNumber(account.getAccountNumber())
                .accountHolderFirstName(account.getUser().getFirstName())
                .accountHolderLastName(account.getUser().getLastName())
                .bankBranch(account.getBranchName())
                .build();
    }

    @Override
    public List<TransactionResponse> getTransactionHistory(String accountNumber) {

        Account account = accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));

        return transactionRepository
                .findByAccountOrderByCreatedAtDesc(account)
                .stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    private String generateReference() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private TransactionResponse mapToTransactionResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionReference(transaction.getTransactionReference())
                .transactionType(transaction.getTransactionType())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .balanceAfterTransaction(transaction.getBalanceAfterTransaction())
                .targetAccountNumber(transaction.getTargetAccountNumber())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    @Override
    public AccountStatementResponse getAccountStatement(
            String accountNumber,
            LocalDateTime fromDate,
            LocalDateTime toDate) {

        Account account = accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found: " + accountNumber));

        List<Transaction> transactions = transactionRepository
                .findByAccountAndCreatedAtBetweenOrderByCreatedAtDesc(
                        account, fromDate, toDate);

        BigDecimal totalDeposits = transactionRepository.sumByAccountAndTypeAndDateRange(
                account, TransactionType.DEPOSIT, fromDate, toDate);

        BigDecimal totalWithdrawals = transactionRepository.sumByAccountAndTypeAndDateRange(
                account, TransactionType.WITHDRAWAL, fromDate, toDate);

        BigDecimal totalTransferDebits = transactionRepository.sumByAccountAndTypeAndDateRange(
                account, TransactionType.TRANSFER_DEBIT, fromDate, toDate);

        BigDecimal totalTransferCredits = transactionRepository.sumByAccountAndTypeAndDateRange(
                account, TransactionType.TRANSFER_CREDIT, fromDate, toDate);

        BigDecimal totalIn = totalDeposits.add(totalTransferCredits);
        BigDecimal totalOut = totalWithdrawals.add(totalTransferDebits);
        BigDecimal netFlow = totalIn.subtract(totalOut);

        return AccountStatementResponse.builder()
                .accountNumber(account.getAccountNumber())
                .accountHolderName(
                        account.getUser().getFirstName() + " "
                                + account.getUser().getLastName())
                .currentBalance(account.getBalance())
                .fromDate(fromDate)
                .toDate(toDate)
                .totalDeposits(totalIn)
                .totalWithdrawals(totalOut)
                .netFlow(netFlow)
                .transactions(transactions.stream()
                        .map(this::mapToTransactionResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    @Transactional
    public AccountResponse updatePrimaryAccount(Long userId, String accountNumber) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + userId));

        List<Account> userAccounts = accountRepository.findByUser(user);

        Account newPrimary = userAccounts.stream()
                .filter(a -> a.getAccountNumber().equals(accountNumber))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found or does not belong to this user"));

        userAccounts.forEach(a -> {
            a.setPrimary(false);
            accountRepository.save(a);
        });

        newPrimary.setPrimary(true);
        Account saved = accountRepository.save(newPrimary);

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
}