package com.BankingSystem.dto.request.debitcard;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class DebitCardPinChangeRequest {

    @NotBlank(message = "Card number is required")
    private String cardNumber;

    @NotBlank(message = "Current PIN is required")
    @Pattern(regexp = "^[0-9]{4}$", message = "PIN must be  4 digits only")
    private String currentPin;

    @NotBlank(message = "New PIN is required")
    @Pattern(regexp = "^[0-9]{4}$", message = "PIN must be exactly 4 digits")
    private String newPin;
}
