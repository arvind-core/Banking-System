package com.BankingSystem.util;

import com.BankingSystem.entity.loan.EMISchedule;

public interface EMISchedulerService {

    void processEmisDueToday();

    void processRetryEmis();

    void processIndividualEmi(EMISchedule emi);

    void imposePenalty(Long emiScheduleId);

    void sendEmiReminders();
}