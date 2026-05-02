package com.BankingSystem.util;

import com.BankingSystem.dto.response.emis.EMIScheduleResponse;
import com.BankingSystem.entity.loan.EMIStatus;
import com.BankingSystem.entity.loan.InterestMethod;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EMICalculator {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final MathContext MC = new MathContext(10, ROUNDING);

    public static BigDecimal calculateEMI(BigDecimal principal,
                                          BigDecimal annualRate,
                                          int tenureMonths,
                                          InterestMethod method) {
        if (method == InterestMethod.FLAT_RATE) {
            return calculateFlatRateEMI(principal, annualRate, tenureMonths);
        }
        return calculateReducingBalanceEMI(principal, annualRate, tenureMonths);
    }

    private static BigDecimal calculateReducingBalanceEMI(BigDecimal principal,
                                                          BigDecimal annualRate,
                                                          int tenureMonths) {
        // R = monthly rate = annual rate / 12 / 100
        BigDecimal monthlyRate = annualRate
                .divide(BigDecimal.valueOf(12), 10, ROUNDING)
                .divide(BigDecimal.valueOf(100), 10, ROUNDING);

        // (1 + R)^N
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRPowN = onePlusR.pow(tenureMonths, MC);

        // EMI = P × R × (1+R)^N / ((1+R)^N - 1)
        BigDecimal numerator = principal
                .multiply(monthlyRate)
                .multiply(onePlusRPowN);

        BigDecimal denominator = onePlusRPowN.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, SCALE, ROUNDING);
    }

    private static BigDecimal calculateFlatRateEMI(BigDecimal principal,
                                                   BigDecimal annualRate,
                                                   int tenureMonths) {
        // Total interest = P × R% × years
        BigDecimal years = BigDecimal.valueOf(tenureMonths)
                .divide(BigDecimal.valueOf(12), 10, ROUNDING);

        BigDecimal totalInterest = principal
                .multiply(annualRate)
                .multiply(years)
                .divide(BigDecimal.valueOf(100), SCALE, ROUNDING);

        // EMI = (P + total interest) / N
        return principal.add(totalInterest)
                .divide(BigDecimal.valueOf(tenureMonths), SCALE, ROUNDING);
    }

    public static BigDecimal calculateTotalInterest(BigDecimal principal,
                                                    BigDecimal annualRate,
                                                    int tenureMonths,
                                                    InterestMethod method) {
        BigDecimal emi = calculateEMI(principal, annualRate, tenureMonths, method);
        BigDecimal totalPayable = emi.multiply(BigDecimal.valueOf(tenureMonths));
        return totalPayable.subtract(principal).setScale(SCALE, ROUNDING);
    }

    public static List<EMIScheduleResponse> generateAmortizationSchedule(
            BigDecimal principal,
            BigDecimal annualRate,
            int tenureMonths,
            InterestMethod method,
            LocalDate startDate) {

        List<EMIScheduleResponse> schedule = new ArrayList<>();
        BigDecimal emi = calculateEMI(principal, annualRate, tenureMonths, method);
        BigDecimal outstanding = principal;

        BigDecimal monthlyRate = annualRate
                .divide(BigDecimal.valueOf(12), 10, ROUNDING)
                .divide(BigDecimal.valueOf(100), 10, ROUNDING);

        for (int i = 1; i <= tenureMonths; i++) {
            BigDecimal interestComponent;
            BigDecimal principalComponent;

            if (method == InterestMethod.REDUCING_BALANCE) {
                // Interest = outstanding × monthly rate
                interestComponent = outstanding
                        .multiply(monthlyRate)
                        .setScale(SCALE, ROUNDING);
                principalComponent = emi.subtract(interestComponent);
            } else {
                // Flat rate — interest is same every month
                interestComponent = principal
                        .multiply(annualRate)
                        .divide(BigDecimal.valueOf(100), 10, ROUNDING)
                        .divide(BigDecimal.valueOf(12), SCALE, ROUNDING);
                principalComponent = emi.subtract(interestComponent);
            }

            // Handle last installment rounding
            if (i == tenureMonths) {
                principalComponent = outstanding;
                emi = principalComponent.add(interestComponent);
            }

            outstanding = outstanding.subtract(principalComponent)
                    .max(BigDecimal.ZERO);

            schedule.add(EMIScheduleResponse.builder()
                    .installmentNumber(i)
                    .dueDate(startDate.plusMonths(i))
                    .emiAmount(emi.setScale(SCALE, ROUNDING))
                    .principalComponent(principalComponent.setScale(SCALE, ROUNDING))
                    .interestComponent(interestComponent.setScale(SCALE, ROUNDING))
                    .outstandingPrincipalAfter(outstanding.setScale(SCALE, ROUNDING))
                    .status(EMIStatus.PENDING)
                    .build());
        }

        return schedule;
    }
}
