package com.BankingSystem.service.savings_account_interest;

import com.BankingSystem.BankConfig;
import com.BankingSystem.entity.account.Account;
import com.BankingSystem.entity.account.AccountType;
import com.BankingSystem.entity.account.DailyBalanceSnapshot;
import com.BankingSystem.entity.transactions.Transaction;
import com.BankingSystem.entity.transactions.TransactionStatus;
import com.BankingSystem.entity.transactions.TransactionType;
import com.BankingSystem.repo.AccountRepository;
import com.BankingSystem.repo.DailyBalanceSnapshotRepository;
import com.BankingSystem.repo.TransactionRepository;
import com.BankingSystem.service.bank.BankLedgerService;
import com.BankingSystem.util.NotificationEvent;
import com.BankingSystem.util.NotificationEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class SavingsInterestServiceImpl implements SavingsInterestService {

    private final AccountRepository accountRepository;
    private final DailyBalanceSnapshotRepository snapshotRepository;
    private final TransactionRepository transactionRepository;
    private final BankLedgerService bankLedgerService;
    private final ApplicationEventPublisher eventPublisher;

    // Runs every night at 11 PM - records closing balance for active account
    @Scheduled(cron = "0 0 23 * * *")
    @Override
    public void recordDailyBalanceSnapshots() {

        LocalDate today = LocalDate.now();
        log.info("Daily balance snapshot scheduler started for date : {}", today);

        List<Account> activeAccount = accountRepository.findAllByIsActiveTrue();

        int recorded = 0;
        int skipped = 0;

        for(Account account : activeAccount) {
            try{
                recordSnapshotForAccount(account, today);
                recorded++;
            }
            catch (Exception e){
                // Unique constraint violation means snapshot already exists
                // This is our idempotency guard - safe to skip
                skipped++;
                log.debug("Snapshot already exists for account {} on {}. Skipping.", account.getAccountNumber(), today);
            }
        }
        log.info("Daily snapshot complete. Recorded : {} | Skipped (already exists) : {}", recorded, skipped);
    }

    // Runs on last day of every month at 11:59 PM - credits interest
    @Scheduled(cron = "0 59 23 L * *")
    @Override
    public void calculateAndCreditMonthlyInterest() {

        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        log.info("Monthly interest scheduler started for period : {} to {}",  monthStart, monthEnd);

        // Only process SAVINGS account - CURRENT accounts earn 0%
        List<Account> savingsAccount = accountRepository.findAllByAccountTypeAndIsActiveTrue(AccountType.SAVINGS);

        log.info("Found {} active savings accounts for interest calculation", savingsAccount.size());

        int credited = 0;
        int skipped = 0;

        for(Account account : savingsAccount) {
            try{
                boolean wasProcessed = creditInterestForAccount(account, monthStart, monthEnd);
                if(wasProcessed) credited++;
                else skipped++;
            }
            catch (Exception e){
                log.error("Failed to credit interest for account {} | Error : {}", account.getAccountNumber(), e.getMessage());
            }
        }
        log.info("Monthly interest complete. Credited : {} | Skipped : {}", credited, skipped);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSnapshotForAccount(Account account, LocalDate date) {
        // Idempotency at java level - double check before DB insert
        if(snapshotRepository.existsByAccountAndSnapshotDate(account, date)){
            return;
        }

        DailyBalanceSnapshot snapshot = DailyBalanceSnapshot.builder()
                .account(account)
                .snapshotDate(date)
                .closingBalance(account.getBalance())
                .build();

        snapshotRepository.save(snapshot);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean creditInterestForAccount(Account account, LocalDate monthStart, LocalDate monthEnd) {

        // Idempotency check - has interest already been credited this month?
        if(account.getLastInterestCreditDate() != null && YearMonth.from(account.getLastInterestCreditDate()).equals(YearMonth.from(monthEnd))){
            log.debug("Interest already credited for account {} in {}. Skipping.",  account.getAccountNumber(), monthEnd);
            return false;
        }

        // Sum all daily closing balances for month
        BigDecimal sumOfDailyBalances = snapshotRepository.sumBalancesForPeriod(account, monthStart, monthEnd);

        if(sumOfDailyBalances.compareTo(BigDecimal.ZERO) <= 0){
            log.debug("No balance data for account {}. Skipping interest.", account.getAccountNumber());
            return false;
        }

        int daysInMonth = monthEnd.getDayOfMonth();

        // Interest = (Sum of daily balances * Annual Rate) / (365 * 100)
        // This is equivalent to summing daily interests : balance * rate / 365 / 100 per day

        BigDecimal annualRate = BigDecimal.valueOf(BankConfig.SAVINGS_ACCOUNT_INTEREST_RATE);

        BigDecimal interest = sumOfDailyBalances.multiply(annualRate).divide(BigDecimal.valueOf(365L * 100), 6, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);

        // Minimum interest threshold - don't credit if less than ₹1
        if(interest.compareTo(BigDecimal.ONE) < BankConfig.MIN_INTEREST_CREDIT_AMOUNT){
            log.debug("Interest amount ₹{} below threshold for account {}. Skipping.", interest, account.getAccountNumber());
            return false;
        }

        // Credit interest to account
        account.setBalance(account.getBalance().add(interest));
        account.setLastInterestCreditDate(monthEnd);
        accountRepository.save(account);

        // Record as DEPOSIT transaction
        String reference = "INT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Transaction interestTransaction = Transaction.builder()
                .transactionReference(reference)
                .transactionType(TransactionType.MONTHLY_INTEREST_ON_SAVINGS_ACCOUNT)
                .status(TransactionStatus.SUCCESS)
                .amount(interest)
                .balanceAfterTransaction(account.getBalance())
                .account(account)
                .description("Monthly interest credit for " + YearMonth.from(monthEnd))
                .build();

        transactionRepository.save(interestTransaction);

        // Update bank ledger - interest is bank's expense, increase deposits
        bankLedgerService.onDeposit(interest);

        // Notify User
        Map<String, Object> data = new HashMap<>();
        data.put("interestAmount", interest);
        data.put("accountNumber", account.getAccountNumber());
        data.put("period", YearMonth.from(monthEnd).toString());
        data.put("newBalance", account.getBalance());
        data.put("annualRate", BankConfig.SAVINGS_ACCOUNT_INTEREST_RATE);

        eventPublisher.publishEvent(NotificationEvent.forUser(this,
                NotificationEventType.INTEREST_CREDITED,
                account.getUser().getFirstName() + " " + account.getUser().getLastName(),
                account.getUser().getEmail(),
                account.getUser().getPhoneNumber(),
                data));

        log.info("Interest ₹{} credited to account {} for period {} to {}.", interest, account.getAccountNumber(), monthStart, monthEnd);
        log.info("Sum of daily balances : ₹{}. Days : {}.", sumOfDailyBalances, daysInMonth);

        return true;
    }
}