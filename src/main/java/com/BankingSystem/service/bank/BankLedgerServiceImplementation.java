package com.BankingSystem.service.bank;

import com.BankingSystem.dto.response.BankLedgerResponse;
import com.BankingSystem.entity.bank.BankLedger;
import com.BankingSystem.exception.InvalidOperationException;
import com.BankingSystem.repo.BankLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankLedgerServiceImplementation implements BankLedgerService{

    private final BankLedgerRepository bankLedgerRepository;

    @Override
    public boolean canLend(BigDecimal amount) {
        BankLedger ledger = bankLedgerRepository.findLedger().orElseThrow(() -> new RuntimeException("Bank Ledger not initialized"));

        return ledger.canLend(amount);
    }

    @Override
    @Transactional
    public void onLoanDisbursed(BigDecimal amount) {
        BankLedger ledger = bankLedgerRepository.findLedgerWithLock().orElseThrow(() -> new RuntimeException("Bank ledger not Initialized"));

        if (!ledger.canLend(amount)){
            throw new InvalidOperationException("Bank does not have sufficient lending capacity " +
                    "to disburse this loan.");
        }

        ledger.setTotalLoanBook(ledger.getTotalLoanBook().add(amount));
        ledger.setTotalDeposits(ledger.getTotalDeposits().subtract(amount));
        bankLedgerRepository.save(ledger);

        log.info("Ledger updated - loan disbursed: ₹{}. " +
                "Available lending capacity: ₹{}",
                amount,ledger.getAvailableLendingCapacity());

    }

    @Override
    @Transactional
    public void onLoanRepaid(BigDecimal amount) {
        BankLedger ledger = bankLedgerRepository.findLedgerWithLock().orElseThrow(() -> new RuntimeException("Bank ledger not initialized"));
        ledger.setTotalLoanBook(ledger.getTotalLoanBook().subtract(amount).max(BigDecimal.ZERO));
        ledger.setTotalDeposits(ledger.getTotalDeposits().add(amount));
        bankLedgerRepository.save(ledger);

        log.info("Ledger updated - loan repaid : ₹{}.",amount);

    }

    @Override
    @Transactional
    public void onCreditCardSpend(BigDecimal amount) {
        BankLedger ledger = bankLedgerRepository.findLedgerWithLock().orElseThrow(() -> new RuntimeException("Bank ledger not initialized"));

        if (!ledger.canLend(amount)){
            throw new InvalidOperationException(
                    "Bank does not have sufficient capacity" +
                            "to process this credit card transaction.");
        }

        ledger.setTotalCreditExposure(ledger.getTotalCreditExposure().add(amount));
        bankLedgerRepository.save(ledger);

        log.info("Ledger updated - credit card spend : ₹{}.",amount);

    }

    @Override
    @Transactional
    public void onCreditCardPayment(BigDecimal amount) {
        BankLedger ledger = bankLedgerRepository.findLedgerWithLock().orElseThrow(() -> new RuntimeException("Bank ledger not initialized"));

        ledger.setTotalCreditExposure(ledger.getTotalCreditExposure().subtract(amount).max(BigDecimal.ZERO));
        ledger.setTotalDeposits(ledger.getTotalDeposits().add(amount));
        bankLedgerRepository.save(ledger);

        log.info("Ledger updated - credit card payment : ₹{}.",amount);
    }

    @Override
    @Transactional
    public void onDeposit(BigDecimal amount) {
        BankLedger ledger = bankLedgerRepository.findLedgerWithLock().orElseThrow(() -> new RuntimeException("Bank ledger not initialized"));

        ledger.setTotalDeposits(ledger.getTotalDeposits().add(amount));
        bankLedgerRepository.save(ledger);

    }

    @Override
    @Transactional
    public void onWithdrawal(BigDecimal amount) {
        BankLedger ledger = bankLedgerRepository.findLedgerWithLock().orElseThrow(() -> new RuntimeException("Bank ledger not initialized"));

        ledger.setTotalDeposits(ledger.getTotalDeposits().subtract(amount).max(BigDecimal.ZERO));
        bankLedgerRepository.save(ledger);

    }

    @Override
    public BankLedgerResponse getLedgerStatus() {
        BankLedger ledger = bankLedgerRepository.findLedgerWithLock().orElseThrow(() -> new RuntimeException("Bank ledger not initialized"));

        BigDecimal totalFunds = ledger.getTotalCapital().add(ledger.getTotalDeposits());
        BigDecimal currentExposure = ledger.getTotalLoanBook().add(ledger.getTotalCreditExposure());

        double utilizationPercentage = totalFunds.compareTo(BigDecimal.ZERO) > 0
                ? currentExposure.divide(totalFunds,4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

        return BankLedgerResponse.builder()
                .totalCapital(ledger.getTotalCapital())
                .totalDeposits(ledger.getTotalDeposits())
                .totalLoanBook(ledger.getTotalLoanBook())
                .totalCreditExposure(ledger.getTotalCreditExposure())
                .availableLendingCapacity(ledger.getAvailableLendingCapacity())
                .totalReserve(ledger.getTotalReserve())
                .lendingUtilizationPercentage(utilizationPercentage)
                .lastUpdated(ledger.getLastUpdated())
                .build();
    }
}
