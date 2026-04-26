package com.BankingSystem.service.trust;

public interface TrustScoreService {

    void increaseScore(Long userId, int points, String reason);

    void decreaseScore(Long userId, int points, String reason);

    int getCurrentScore(Long userId);

    String getScoreCategory(int score);

    boolean isEligibleForLoan(Long userId);
}
