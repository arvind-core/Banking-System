package com.BankingSystem.dto.request.creditcard;

import com.BankingSystem.entity.card.CardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreditCardApplicationRequest {
    @NotBlank(message = "Account Number is required")
    private String accountNumber;

    @NotNull(message = "Card Type is required")
    private CardType cardType;
}
