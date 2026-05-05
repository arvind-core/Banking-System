package com.BankingSystem.Controller;

import com.BankingSystem.dto.request.creditcard.CardPaymentRequest;
import com.BankingSystem.dto.request.creditcard.CreditCardApplicationRequest;
import com.BankingSystem.dto.request.creditcard.LimitIncreaseRequest;
import com.BankingSystem.dto.response.creditcard.BillingCycleResponse;
import com.BankingSystem.dto.response.creditcard.CardTransactionResponse;
import com.BankingSystem.dto.response.creditcard.CreditCardResponse;
import com.BankingSystem.entity.card.CreditCard;
import com.BankingSystem.service.creditcard.CreditCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CreditCardController {
    private final CreditCardService creditCardService;

    @PostMapping("/apply")
    public ResponseEntity<CreditCardResponse> applyForCard(@Valid @RequestBody CreditCardApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(creditCardService.applyForCard(request));
    }

    @PatchMapping("/approve/{cardNumber}")
    public ResponseEntity<CreditCardResponse> approveCard(@PathVariable String cardNumber, @RequestParam Long managerId){
        return ResponseEntity.ok(creditCardService.approveCard(cardNumber, managerId));
    }

    @PatchMapping("/reject/{cardNumber}")
    public ResponseEntity<CreditCardResponse> rejectCard(@PathVariable String cardNumber, @RequestParam Long managerId, String reason) {
        return ResponseEntity.ok(creditCardService.rejectCard(cardNumber, managerId, reason));
    }

    @PostMapping("/payment")
    public ResponseEntity<CreditCardResponse> makePayment(@Valid @RequestBody CardPaymentRequest request){
        return ResponseEntity.ok(creditCardService.makePayment(request));
    }

    @PatchMapping("/block/{cardNumber}")
    public ResponseEntity<CreditCardResponse> blockCard(@PathVariable String cardNumber){
        return ResponseEntity.ok(creditCardService.blockCard(cardNumber));
    }

    @PatchMapping("/unblock/{cardNumber}")
    public ResponseEntity<CreditCardResponse> unblockCard(@PathVariable String cardNumber, @RequestParam Long managerId){
        return ResponseEntity.ok(creditCardService.unblockCard(cardNumber, managerId));
    }

    @PatchMapping("/limit-increase")
    public ResponseEntity<CreditCardResponse> requestLimitIncrease(@Valid @RequestBody LimitIncreaseRequest request){
        return ResponseEntity.ok(creditCardService.requestLimitIncrease(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CreditCardResponse>> getUserCards(@PathVariable Long userId){
        return ResponseEntity.ok(creditCardService.getUserCards(userId));
    }

    @GetMapping("/transactions/{cardNumber}")
    public ResponseEntity<List<CardTransactionResponse>> getCardTransactions(@PathVariable String cardNumber){
        return ResponseEntity.ok(creditCardService.getCardTransactions(cardNumber));
    }

    @GetMapping("/billing-history/{cardNumber}")
    public ResponseEntity<List<BillingCycleResponse>> getBillingHistory(@PathVariable String cardNumber){
        return ResponseEntity.ok(creditCardService.getBillingHistory(cardNumber));
    }

    @GetMapping("/billing-current/{cardNumber}")
    public ResponseEntity<BillingCycleResponse> getCurrentBillingCycle(@PathVariable String cardNumber){
        return ResponseEntity.ok(creditCardService.getCurrentBillingCycle(cardNumber));
    }
}