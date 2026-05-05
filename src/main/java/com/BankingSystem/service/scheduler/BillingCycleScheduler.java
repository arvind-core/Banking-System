package com.BankingSystem.service.scheduler;

import com.BankingSystem.entity.card.BillingCycle;
import com.BankingSystem.entity.card.BillingStatus;
import com.BankingSystem.entity.card.CardStatus;
import com.BankingSystem.entity.card.CreditCard;
import com.BankingSystem.repo.BillingCycleRepository;
import com.BankingSystem.repo.CreditCardRepository;
import com.BankingSystem.service.trust.TrustScoreService;
import com.BankingSystem.util.NotificationEvent;
import com.BankingSystem.util.NotificationEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.BankingSystem.BankConfig.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingCycleScheduler {
    private final BillingCycleRepository billingCycleRepository;
    private final CreditCardRepository creditCardRepository;
    private final TrustScoreService trustScoreService;
    private final ApplicationEventPublisher eventPublisher;

    // Runs on 1st of every month at 1 AM - generates statements
    @Scheduled(cron = "0 0 1 1 * *")
    public void generateMonthlyStatements(){
        log.info("Billing cycle scheduler started - generating monthly statements.");

        List<BillingCycle> openCycles = billingCycleRepository.findAllOpenCycles();

        log.info("Found {} open billing cycles to close", openCycles.size());

        for(BillingCycle cycle : openCycles){
            try{
                closeAndGenerateStatement(cycle);
            }
            catch (Exception e){
                log.error("Failed to generate statement for cycle id : {} | Error. {}", cycle.getId(), e.getMessage() );
            }
        }
    }

    // Runs daily at 2 AM - checks overdue bills and charges interest
    @Scheduled(cron = "0 0 2 * * *")
    public void processOverdueBills(){
        log.info("Billing cycle scheduler started - processing overdue bills.");

        List<BillingCycle> overdueCycles = billingCycleRepository.findOverdueCycles(LocalDate.now());

        log.info("Found {} overdue billing cycles", overdueCycles.size());

        for(BillingCycle cycle : overdueCycles){
            try {
                processOverdueCycle(cycle);
            }
            catch (Exception e){
                log.error("Failed to process overdue cycle id : {} | Error : {} ",  cycle.getId(), e.getMessage());
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void closeAndGenerateStatement(BillingCycle cycle){

        CreditCard card = cycle.getCreditCard();

        // Idempotency check - don't process already closed cycles
        if(cycle.getStatus() != BillingStatus.OPEN){
            log.info("Cycle {} already closed. Skipping.", cycle.getId());
            return;
        }

        BigDecimal closingOutstanding = cycle.getOpeningOutstanding().add(cycle.getTotalSpend());

        BigDecimal minimumDue = calculateMinimumDue(closingOutstanding);

        cycle.setClosingOutstanding(closingOutstanding);
        cycle.setMinimumDue(minimumDue);
        cycle.setStatus(BillingStatus.GENERATED);
        billingCycleRepository.save(cycle);

        // update card minimum due
        card.setMinimumDue(minimumDue);

        card.setNextBillingDate(LocalDate.now().plusMonths(1).withDayOfMonth(1));
        card.setPaymentDueDate(LocalDate.now().plusDays(CREDIT_CARD_PAYMENT_DUE_DAYS));

        creditCardRepository.save(card);

        // Open new billing cycle for next month
        openNewBillingCycle(card, closingOutstanding);

        // Send statement notification
        Map<String, Object> data = new HashMap<>();
        data.put("totalSpend", closingOutstanding);
        data.put("minimumDue", minimumDue);
        data.put("closingOutstanding", closingOutstanding);
        data.put("paymentDueDate", card.getPaymentDueDate().toString());
        data.put("maskedCardNumber", card.getMaskedCardNumber());

        eventPublisher.publishEvent(NotificationEvent.forUser(this,
                NotificationEventType.BILLING_CYCLE_GENERATED,
                card.getCardHolderName(),
                card.getUser().getEmail(),
                card.getUser().getPhoneNumber(),
                data));

        log.info("Statement generated for card {}. Outstanding: ₹{}. Min due: ₹{}.",card.getMaskedCardNumber(),closingOutstanding, minimumDue);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOverdueCycle(BillingCycle cycle){
        CreditCard card = cycle.getCreditCard();
        LocalDate today = LocalDate.now();

        long daysOverdue = ChronoUnit.DAYS.between(card.getPaymentDueDate(), today);

        cycle.setDaysOverdue((int) daysOverdue);

        // Check if minimum due was paid
        boolean minimumPaid = cycle.getTotalPaid().compareTo(cycle.getMinimumDue()) >= 0;

        if(!minimumPaid){
            // Charge late payment fee - only once on first day overdue
            if(daysOverdue == 1 && cycle.getLatePaymentFee().compareTo(BigDecimal.ZERO) == 0){
                BigDecimal lateFee = BigDecimal.valueOf(LATE_PAYMENT_FEE);
                cycle.setLatePaymentFee(lateFee);
                card.setOutstandingAmount(card.getOutstandingAmount().add(lateFee));

                log.info("Late payment fee ₹{} charged for card {}", lateFee, card.getMaskedCardNumber());
            }

            // Calculate and charge daily interest
            BigDecimal dailyRate = BigDecimal.valueOf(CREDIT_CARD_LATE_PENALTY_ANNUAL / 100 / 365);

            BigDecimal dailyInterest = cycle.getClosingOutstanding()
                    .subtract(cycle.getTotalPaid())
                    .multiply(dailyRate)
                    .setScale(2, RoundingMode.HALF_UP);

            cycle.setInterestCharged(cycle.getInterestCharged().add(dailyInterest));
            card.setOutstandingAmount(card.getOutstandingAmount().add(dailyRate));

            // Update status
            cycle.setStatus(BillingStatus.GENERATED);

            // Decrease trust score for missed payment
            trustScoreService.decreaseScore(card.getUser().getId(), Math.abs(SCORE_CARD_MIN_DUE_MISSED), "Credit card minimum due missed");

            // Track consecutive missed payments
            card.setConsecutiveMissedPayments(card.getConsecutiveMissedPayments() + 1);

            // Auto-block after threshold
            if(card.getConsecutiveMissedPayments() >= AUTO_BLOCK_AFTER_MISSED_PAYMENTS){
                card.setStatus(CardStatus.BLOCKED);

                log.warn("Card {} auto-blocked after {} consecutive missed payments", card.getMaskedCardNumber(), card.getConsecutiveMissedPayments());
            }

            Map<String, Object> blockData = new HashMap<>();
            blockData.put("maskedCardNumber", card.getMaskedCardNumber());
            blockData.put("reason","Consecutive missed payments");

            eventPublisher.publishEvent(NotificationEvent.forUser(this,
                    NotificationEventType.CARD_BLOCKED,
                    card.getCardHolderName(),
                    card.getUser().getEmail(),
                    card.getUser().getPhoneNumber(),
                    blockData));

            // Send overdue notification
            Map<String, Object> data = new HashMap<>();
            data.put("daysOverdue", daysOverdue);
            data.put("outstandingAmount", card.getOutstandingAmount());
            data.put("interestCharged",cycle.getInterestCharged());
            data.put("latePaymentFee",cycle.getLatePaymentFee());
            data.put("maskedCardNumber", card.getMaskedCardNumber());

            eventPublisher.publishEvent(NotificationEvent.forUser(this,
                    NotificationEventType.PAYMENT_DUE_REMINDER,
                    card.getCardHolderName(),
                    card.getUser().getEmail(),
                    card.getUser().getPhoneNumber(),
                    data));
        }
        billingCycleRepository.save(cycle);
        creditCardRepository.save(card);
    }

    private void openNewBillingCycle(CreditCard card, BigDecimal openingOutstanding){
        LocalDate today = LocalDate.now();
        LocalDate cycleEnd = today.plusMonths(1).withDayOfMonth(1).minusDays(1);

        LocalDate dueDate = today.plusMonths(1).withDayOfMonth(CREDIT_CARD_PAYMENT_DUE_DAYS);

        BillingCycle newCycle = BillingCycle.builder()
                .creditCard(card)
                .cycleStartDate(today)
                .cycleEndDate(cycleEnd)
                .paymentDueDate(dueDate)
                .totalSpend(BigDecimal.ZERO)
                .minimumDue(BigDecimal.ZERO)
                .openingOutstanding(openingOutstanding)
                .closingOutstanding(openingOutstanding)
                .totalPaid(BigDecimal.ZERO)
                .interestCharged(BigDecimal.ZERO)
                .latePaymentFee(BigDecimal.ZERO)
                .totalRewardPointsEarned(BigDecimal.ZERO)
                .daysOverdue(0)
                .status(BillingStatus.OPEN)
                .build();

        billingCycleRepository.save(newCycle);
        log.info("New billing cycle opened for card {}", card.getMaskedCardNumber());
    }

    private BigDecimal calculateMinimumDue(@NonNull BigDecimal outstanding){
        if(outstanding.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

        BigDecimal percentage = outstanding.multiply(BigDecimal.valueOf(CREDIT_CARD_MIN_DUE_PERCENTAGE / 100));

        BigDecimal floor = BigDecimal.valueOf(CREDIT_CARD_MIN_DUE_FLOOR);

        return percentage.max(floor).setScale(2, RoundingMode.HALF_UP);
    }
}