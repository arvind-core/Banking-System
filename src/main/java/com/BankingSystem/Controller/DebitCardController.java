package com.BankingSystem.Controller;

import com.BankingSystem.dto.request.debitcard.DebitCardPinChangeRequest;
import com.BankingSystem.dto.request.debitcard.DebitCardRequest;
import com.BankingSystem.dto.request.debitcard.DebitCardTransactionRequest;
import com.BankingSystem.dto.response.debitcard.DebitCardResponse;
import com.BankingSystem.dto.response.debitcard.DebitCardTransactionResponse;
import com.BankingSystem.service.debitcard.DebitCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/debit-cards")
@RequiredArgsConstructor
public class DebitCardController {

    private final DebitCardService debitCardService;

    @PostMapping("/issue")
    public ResponseEntity<DebitCardResponse> issueCard(
            @Valid @RequestBody DebitCardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(debitCardService.issueCard(request));
    }

    @PostMapping("/transaction")
    public ResponseEntity<DebitCardTransactionResponse> processTransaction(
            @Valid @RequestBody DebitCardTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(debitCardService.processTransaction(request));
    }

    @PatchMapping("/block/{cardNumber}")
    public ResponseEntity<DebitCardResponse> blockCard(
            @PathVariable String cardNumber) {
        return ResponseEntity.ok(debitCardService.blockCard(cardNumber));
    }

    @PatchMapping("/unblock/{cardNumber}")
    public ResponseEntity<DebitCardResponse> unblockCard(
            @PathVariable String cardNumber) {
        return ResponseEntity.ok(debitCardService.unblockCard(cardNumber));
    }

    @PatchMapping("/change-pin")
    public ResponseEntity<DebitCardResponse> changePin(
            @Valid @RequestBody DebitCardPinChangeRequest request) {
        return ResponseEntity.ok(debitCardService.changePin(request));
    }

    @PatchMapping("/toggle-international/{cardNumber}")
    public ResponseEntity<DebitCardResponse> toggleInternational(
            @PathVariable String cardNumber,
            @RequestParam boolean enabled) {
        return ResponseEntity.ok(
                debitCardService.toggleInternationalTransactions(
                        cardNumber, enabled));
    }

    @PatchMapping("/toggle-online/{cardNumber}")
    public ResponseEntity<DebitCardResponse> toggleOnline(
            @PathVariable String cardNumber,
            @RequestParam boolean enabled) {
        return ResponseEntity.ok(
                debitCardService.toggleOnlineTransactions(
                        cardNumber, enabled));
    }

    @GetMapping("/{cardNumber}")
    public ResponseEntity<DebitCardResponse> getCard(
            @PathVariable String cardNumber) {
        return ResponseEntity.ok(
                debitCardService.getCardDetails(cardNumber));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DebitCardResponse>> getUserCards(
            @PathVariable Long userId) {
        return ResponseEntity.ok(debitCardService.getUserCards(userId));
    }

    @GetMapping("/transactions/{cardNumber}")
    public ResponseEntity<List<DebitCardTransactionResponse>>
            getTransactionHistory(@PathVariable String cardNumber) {
        return ResponseEntity.ok(
                debitCardService.getTransactionHistory(cardNumber));
    }
}