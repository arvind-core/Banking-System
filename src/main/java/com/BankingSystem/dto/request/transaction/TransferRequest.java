package com.BankingSystem.dto.request.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransferRequest {

    @NotBlank(message = "Sender account number is required")
    private String senderAccountNumber;

    @NotBlank(message = "Receiver identifier is required")
    private String receiverIdentifier;

    @NotNull(message = "Transfer type is required")
    private TransferType transferType;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Transfer amount must be at least 1")
    private BigDecimal amount;

    private String description;

    public enum TransferType {
        ACCOUNT_NUMBER,
        PHONE_NUMBER
    }
}