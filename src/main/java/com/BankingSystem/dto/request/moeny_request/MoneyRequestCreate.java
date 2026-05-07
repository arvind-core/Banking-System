package com.BankingSystem.dto.request.moeny_request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MoneyRequestCreate {
    @NotBlank(message = "Payer Phone number is required")
    private String payerPhoneNumber;

    @NotBlank(message = "Requester account number is required")
    private String requesterAccountNumber;

    @NotNull(message = "Amount is requried")
    @DecimalMin(value = "1.0", message = "Minimum request amount is ₹1")
    private BigDecimal amount;

    @NotBlank(message = "Description is Required")
    private String description;
}
