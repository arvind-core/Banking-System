package com.BankingSystem.dto.request.debitcard;

import com.BankingSystem.entity.card.DebitCardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class DebitCardRequest {

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotNull(message = "Card type is required")
    private DebitCardType cardType;

    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "^[0-9]{4}$", message = "PIN must be exactly 4 digits")
    private String pin;
}
