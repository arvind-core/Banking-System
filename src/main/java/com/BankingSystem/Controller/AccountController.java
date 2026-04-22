package com.BankingSystem.Controller;

import com.BankingSystem.dto.request.CreateAccountRequest;
import com.BankingSystem.dto.response.AccountResponse;
import com.BankingSystem.service.user.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/register")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/get/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountByNumber(@PathVariable String accountNumber) {
        AccountResponse response = accountService.getAccountByAccountNumber(accountNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AccountResponse>> getAccountsByUser(@PathVariable Long userId) {
        List<AccountResponse> response = accountService.getAccountsByUser(userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/close/{accountNumber}")
    public ResponseEntity<AccountResponse> closeAccount(
            @PathVariable String accountNumber) {
        AccountResponse response = accountService.closeAccount(accountNumber);
        return ResponseEntity.ok(response);
    }
}