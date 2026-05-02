package com.BankingSystem.dto.response.emis;

import com.BankingSystem.entity.loan.InterestMethod;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class EMICalculationPreviewResponse {

    private BigDecimal principalAmount;
    private BigDecimal annualInterestRate;
    private Integer tenureMonths;
    private InterestMethod interestMethod;
    private BigDecimal monthlyEMI;
    private BigDecimal totalInterestPayable;
    private BigDecimal totalAmountPayable;
    private List<EMIScheduleResponse> amortizationSchedule;
}