package com.BankingSystem.Controller;

import com.BankingSystem.dto.request.transaction.DepositRequest;
import com.BankingSystem.dto.request.transaction.TransferRequest;
import com.BankingSystem.dto.request.transaction.WithdrawalRequest;
import com.BankingSystem.dto.response.transaction.BeneficiaryResponse;
import com.BankingSystem.dto.response.transaction.TransactionResponse;
import com.BankingSystem.service.transaction.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody DepositRequest request) {
        TransactionResponse response = transactionService.deposit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(@Valid @RequestBody WithdrawalRequest request) {
        TransactionResponse response = transactionService.withdraw(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request) {
        TransactionResponse response = transactionService.transfer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/beneficiary")
    public ResponseEntity<BeneficiaryResponse> getBeneficiaryDetails(@RequestParam String identifier, @RequestParam String identifierType) {
        BeneficiaryResponse response = transactionService.getBeneficiaryDetails(identifier, identifierType);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/{accountNumber}")
    public ResponseEntity<List<TransactionResponse>> getTransactionHistory(@PathVariable String accountNumber) {
        List<TransactionResponse> response = transactionService.getTransactionHistory(accountNumber);
        return ResponseEntity.ok(response);
    }
}
