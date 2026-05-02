package com.BankingSystem.dto.request.creditcard;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CardPaymentRequest {
    @NotBlank(message = "Card number is required")
    private String cardNumber;

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "1.0",message = "Minimum transaction value is ₹1")
    private BigDecimal paymentAmount;
}
