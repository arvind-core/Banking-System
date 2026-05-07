package com.BankingSystem.dto.response.money_request_response;

import com.BankingSystem.entity.requests.MoneyRequestStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class MoneyRequestResponse {

    private Long id;
    private String requestReference;
    private String requesterName;
    private String requesterAccountNumber;
    private String payerName;
    private BigDecimal amount;
    private String description;
    private MoneyRequestStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime respondedAt;
    private LocalDateTime createdAt;
}
