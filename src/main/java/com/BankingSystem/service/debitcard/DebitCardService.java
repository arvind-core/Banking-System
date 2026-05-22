package com.BankingSystem.service.debitcard;

import com.BankingSystem.dto.request.debitcard.DebitCardPinChangeRequest;
import com.BankingSystem.dto.request.debitcard.DebitCardRequest;
import com.BankingSystem.dto.request.debitcard.DebitCardTransactionRequest;
import com.BankingSystem.dto.response.debitcard.DebitCardResponse;
import com.BankingSystem.dto.response.debitcard.DebitCardTransactionResponse;

import java.util.List;

public interface DebitCardService {

    DebitCardResponse issueCard(DebitCardRequest request);

    DebitCardTransactionResponse processTransaction(DebitCardTransactionRequest request);

    DebitCardResponse blockCard(String cardNumber);

    DebitCardResponse unblockCard(String cardNumber);

    DebitCardResponse changePin(DebitCardPinChangeRequest request);

    DebitCardResponse toggleInternationalTransactions(String cardNumber, boolean enabled);

    DebitCardResponse toggleOnlineTransactions(String cardNumber, boolean enabled);

    DebitCardResponse getCardDetails(String cardNumber);

    List<DebitCardResponse> getUserCards(Long userId);

    List<DebitCardTransactionResponse> getTransactionHistory(String cardNumber);
}
