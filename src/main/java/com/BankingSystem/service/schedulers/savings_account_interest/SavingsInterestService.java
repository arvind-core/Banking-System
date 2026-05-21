package com.BankingSystem.service.schedulers.savings_account_interest;

public interface SavingsInterestService {

    void recordDailyBalanceSnapshots();

    void calculateAndCreditMonthlyInterest();
}
