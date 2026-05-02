package com.BankingSystem.dto.request.creditcard;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LimitIncreaseRequest {

    @NotBlank(message = "Card number is required")
    private String cardNumber;

    @NotNull(message = "Request limit is required")
    @DecimalMin(value = "1000.0", message = "Minimu limit increase is ₹1000")
    private BigDecimal requestedLimit;

    @NotBlank(message = "Reason is required")
    private String reason;

    private Long reviewedByManagerId;
}
