package com.BankingSystem.service.creditcard;

import com.BankingSystem.dto.request.creditcard.CardPaymentRequest;
import com.BankingSystem.dto.request.creditcard.CardTransactionRequest;
import com.BankingSystem.dto.request.creditcard.CreditCardApplicationRequest;
import com.BankingSystem.dto.request.creditcard.LimitIncreaseRequest;
import com.BankingSystem.dto.response.creditcard.BillingCycleResponse;
import com.BankingSystem.dto.response.creditcard.CardTransactionResponse;
import com.BankingSystem.dto.response.creditcard.CreditCardResponse;

import java.util.List;

public interface CreditCardService {

    CreditCardResponse applyForCard(CreditCardApplicationRequest request);

    CreditCardResponse approveCard(String cardNumber,Long managerId);

    CreditCardResponse rejectCard(String cardNumber,Long managerId, String reason);

    CardTransactionResponse processTransaction(CardTransactionRequest request);

    CreditCardResponse makePayment(CardPaymentRequest request);

    CreditCardResponse blockCard(String cardNumber);

    CreditCardResponse unblockCard(String cardNumber, Long managerId);

    CreditCardResponse requestLimitIncrease(LimitIncreaseRequest request);

    List<CreditCardResponse> getUserCards(Long userId);

    List<CardTransactionResponse> getCardTransactions(String cardNumber);

    List<BillingCycleResponse> getBillingHistory(String cardNumber);

    BillingCycleResponse getCurrentBillingCycle(String cardNumber);

}