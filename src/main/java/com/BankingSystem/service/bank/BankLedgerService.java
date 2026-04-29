package com.BankingSystem.service.bank;

import com.BankingSystem.dto.response.BankLedgerResponse;

import java.math.BigDecimal;

public interface BankLedgerService {
    boolean canLend(BigDecimal amount);

    void onLoanDisbursed(BigDecimal amount);

    void onLoanRepaid(BigDecimal amount);

    void onCreditCardSpend(BigDecimal amount);

    void onCreditCardPayment(BigDecimal amount);

    void onDeposit(BigDecimal amount);

    void onWithdrawal(BigDecimal amount);

    BankLedgerResponse getLedgerStatus();
}
