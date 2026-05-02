package com.BankingSystem.dto.request.creditcard;

import com.BankingSystem.entity.card.SpendCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CardTransactionRequest {

    @NotBlank(message = "Card Number is required")
    private String cardNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Minimum transaction amount is ₹1")
    private BigDecimal amount;

    @NotBlank(message = "Merchant name is required")
    private String merchantName;

    @NotNull(message = "Category is required")
    private SpendCategory category;

    private String Description;


}
