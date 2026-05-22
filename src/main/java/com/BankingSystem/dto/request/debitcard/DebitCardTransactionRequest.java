package com.BankingSystem.dto.request.debitcard;

import com.BankingSystem.entity.card.DebitCardTransaction;
import com.BankingSystem.entity.card.DebitTransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DebitCardTransactionRequest {

    @NotBlank(message = "Card number is required")
    private String cardNumber;

    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "^[0-9]{4}$", message = "PIN must be 4 digits")
    private String pin;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Minimum transaction amount is ₹1")
    private BigDecimal amount;

    @NotBlank(message = "Merchant name is required")
    private String merchantName;

    @NotNull(message = "Transaction type is required")
    private DebitTransactionType transactionType;

    private String description;
}
