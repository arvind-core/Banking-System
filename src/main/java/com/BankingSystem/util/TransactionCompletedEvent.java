package com.BankingSystem.util;

import com.BankingSystem.entity.account.Account;
import com.BankingSystem.entity.transactions.Transaction;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TransactionCompletedEvent extends ApplicationEvent {

    private final Transaction transaction;
    private final Account account;
    private final String accountHolderName;
    private final String email;
    private final String phoneNumber;

    public TransactionCompletedEvent(Object source, Transaction transaction, Account account) {
        super(source);
        this.transaction = transaction;
        this.account = account;
        this.accountHolderName = account.getUser().getFirstName()
                + " " + account.getUser().getLastName();
        this.email = account.getUser().getEmail();
        this.phoneNumber = account.getUser().getPhoneNumber();
    }
}