package com.BankingSystem.dto.response.creditcard;

import com.BankingSystem.entity.card.BillingStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class BillingCycleResponse {

    private Long id;
    private LocalDate cycleStartDate;
    private LocalDate cycleEndDate;
    private LocalDate paymentDueDate;
    private BigDecimal totalSpend;
    private BigDecimal minimumDue;
    private BigDecimal openingOutstanding;
    private BigDecimal closingOutstanding;
    private BigDecimal totalPaid;
    private BigDecimal interestCharged;
    private BigDecimal latePaymentFee;
    private BigDecimal totalRewardPointsEarned;
    private BillingStatus status;
    private Integer daysOverdue;
    private LocalDateTime createdAt;
}
