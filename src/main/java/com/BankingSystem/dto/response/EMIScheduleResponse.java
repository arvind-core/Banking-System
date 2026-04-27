package com.BankingSystem.dto.response;

import com.BankingSystem.entity.loan.EMIStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class EMIScheduleResponse {

    private Long id;
    private Integer installmentNumber;
    private LocalDate dueDate;
    private BigDecimal emiAmount;
    private BigDecimal principalComponent;
    private BigDecimal interestComponent;
    private BigDecimal outstandingPrincipalAfter;
    private EMIStatus status;
    private LocalDate paidDate;
    private BigDecimal penaltyAmount;
}