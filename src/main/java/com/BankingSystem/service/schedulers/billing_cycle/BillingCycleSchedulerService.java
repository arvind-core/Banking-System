package com.BankingSystem.service.schedulers.billing_cycle;


import com.BankingSystem.entity.card.BillingCycle;

public interface BillingCycleSchedulerService {

    void generateMonthlyStatements();

    void processOverdueBills();

    void closeAndGenerateStatement(BillingCycle cycle);

    void processOverdueCycle(BillingCycle cycle);
}
