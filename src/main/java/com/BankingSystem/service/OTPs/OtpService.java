package com.BankingSystem.service.OTPs;

public interface OtpService {

    String generatedAndSendOtp(Long userId, String operation);

    boolean verifyOtp(Long userId, String operation, String otp);

    void invalidOtp(Long userId, String operation);
}
