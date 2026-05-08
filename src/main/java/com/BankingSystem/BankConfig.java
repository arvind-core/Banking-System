package com.BankingSystem;

public class BankConfig {

    // Bank Identity
    public static final String BANK_NAME = "Akash Bank";
    public static final String BANK_SHORT_NAME = "AB";
    public static final String BANK_SUPPORT_EMAIL = "support@akashbank.com";
    public static final String BANK_SUPPORT_PHONE = "1800-XXX-XXXX";

    // OTP Configuration
    public static final int OTP_EXPIRY_SECONDS = 120;
    public static final int OTP_LENGTH = 6;
    public static final int MAX_OTP_ATTEMPTS = 3;

    // Transaction Limits
    public static final double MIN_TRANSACTION_AMOUNT = 1.0;
    public static final double MAX_SINGLE_TRANSFER_AMOUNT = 200000.0;
    public static final double OTP_REQUIRED_ABOVE_AMOUNT = 50000.0;

    // Interest Rates (Annual percentage)
    public static final double SAVINGS_ACCOUNT_INTEREST_RATE = 3.5;
    public static final double CURRENT_ACCOUNT_INTEREST_RATE = 0.0;

    // Account Configuration
    public static final int ACCOUNT_NUMBER_LENGTH = 12;

    // Token Configuration
    public static final long ACCESS_TOKEN_EXPIRY_MS = 86400000L;    // 24 hours
    public static final long REFRESH_TOKEN_EXPIRY_MS = 604800000L;  // 7 days

    // Loan Configuration

    public static final int MIN_TENURE_MONTHS_FOR_LOAN = 3;
    public static final int MAX_TENURE_MONTHS_FOR_LOAN = 360;

    public static final double MIN_LOAN_AMOUNT = 10000.0;
    public static final double MAX_PERSONAL_LOAN_AMOUNT = 1000000.0;
    public static final double MAX_HOME_LOAN_AMOUNT = 10000000.0;
    public static final int MAX_LOAN_TENURE_MONTHS = 360; // 30 years

    // Credit Card Configuration
    public static final double CREDIT_CARD_INTEREST_RATE_MONTHLY = 3.5;
    public static final double MIN_DUE_PERCENTAGE = 5.0;
    public static final int BILLING_CYCLE_DAY = 1;
    public static final int PAYMENT_DUE_DAYS_AFTER_BILLING = 15;

    // Branch Transfer
    public static final int BRANCH_TRANSFER_APPROVAL_DAYS = 7;

    // Loan Eligibility age limits\

    public static final int MAX_LOAN_APPLICATIONS = 3;

    public static final int MIN_LOAN_AGE = 21;
    public static final int MAX_LOAN_AGE = 60;
    public static final int MIN_HOME_LOAN_AGE = 21;
    public static final int MAX_HOME_LOAN_AGE = 65;

    public static final int TRUST_SCORE_PREMIUM = 80;
    public static final int TRUST_SCORE_GOOD = 60;
    public static final int TRUST_SCORE_AVERAGE = 40;
    public static final int TRUST_SCORE_RISKY = 20;
    public static final int INITIAL_TRUST_SCORE = 50;

    public static final int SCORE_EMI_PAID_ON_TIME = 2;
    public static final int SCORE_LOAN_FULLY_REPAID = 5;
    public static final int SCORE_CREDIT_CARD_FULL_PAYMENT = 2;
    public static final int SCORE_ACCOUNT_AGE_BONUS = 1;
    public static final int SCORE_ACTIVE_USAGE_BONUS = 1;
    public static final int SCORE_EMI_MISSED = -5;
    public static final int SCORE_EMI_LATE = -2;
    public static final int SCORE_BELOW_MIN_BALANCE = -1;
    public static final int SCORE_MULTIPLE_LOAN_APPLICATIONS = -2;
    public static final int SCORE_CARD_MIN_DUE_MISSED = -3;

    // Standard Loan rates on different loans, discount may be applied in service layer


    public static final double LOAN_INTEREST_RATE_FOR_HOME = 8.5;
    public static final double LOAN_INTEREST_RATE_FOR_CAR = 9.5;
    public static final double LOAN_INTEREST_RATE_FOR_EDUCATION = 8.0;
    public static final double LOAN_INTEREST_RATE_FOR_PERSONAL = 12.0;
    public static final double LOAN_INTEREST_RATE_FOR_BUSINESS = 11.0;
    public static final double LOAN_INTEREST_RATE_FOR_GOLD = 7.5;

    // Bank Capital Configuration

    public static final double MAX_LENDING_RATION = 0.80;
    public static final double CREDIT_CARD_LATE_PENALTY_ANNUAL = 36.0;
    public static final double CREDIT_CARD_LATE_PENALTY_MONTHLY = 3.0;
    public static final int CREDIT_CARD_PAYMENT_DUE_DAYS = 15;
    public static final int CREDIT_CARD_BILLING_CYCLE_DAY = 1;
    public static final double CREDIT_CARD_MIN_DUE_PERCENTAGE = 5.0;
    public static final double CREDIT_CARD_MIN_DUE_FLOOR = 500.0;
    public static final double REWARD_POINTS_PER_100_SPENT = 1.0;



    // Credit Card Limits by CardType

    public static final double CLASSIC_CARD_MAX_LIMIT = 100000.0;
    public static final double GOLD_CARD_MAX_LIMIT = 300000.0;
    public static final double PLATINUM_CARD_MAX_LIMIT = 500000.0;
    public static final double SIGNATURE_CARD_MAX_LIMIT = 1000000.0;

    // Credit Limit income multiplier
    public static final double CREDIT_LIMIT_INCOME_MULTIPLIER = 2.5;

    // Minimum trust score for credit card
    public static final int MIN_TRUST_SCORE_FOR_CREDIT_CARD = 20;

    // Credit card approval thresholds
    public static final String AUTO_APPROVE_BELOW_TYPE = "PLATINUM";
    public static final double LATE_PAYMENT_FEE = 500.0;
    public static final int AUTO_BLOCK_AFTER_MISSED_PAYMENTS = 3;
    public static final int CARD_EXPIRY_YEARS = 5;

    // Reward points multiplier per category
    public static final double REWARD_DINING = 5.0;
    public static final double REWARD_TRAVEL = 5.0;
    public static final double REWARD_FUEL = 2.0;
    public static final double REWARD_GROCERY = 2.0;
    public static final double REWARD_SHOPPING = 3.0;
    public static final double REWARD_DEFAULT = 1.0;

    // Money requests
    public static final long AUTO_EXPIRE_MONEY_REQUEST_HOURS = 24;
}
