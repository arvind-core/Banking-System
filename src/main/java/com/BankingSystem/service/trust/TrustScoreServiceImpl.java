package com.BankingSystem.service.trust;

import com.BankingSystem.BankConfig;
import com.BankingSystem.entity.users.User;
import com.BankingSystem.exception.ResourceNotFoundException;
import com.BankingSystem.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrustScoreServiceImpl implements TrustScoreService{

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void increaseScore(Long userId, int points, String reason) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found : " + userId));

        int newScore = Math.min(100, user.getTrustScore() + points);
        user.setTrustScore(newScore);
        userRepository.save(user);

        log.info("Trust score increased for user {} by {} points. Reason: {}. New score: {}",
                userId, points, reason, newScore);
    }

    @Override
    @Transactional
    public void decreaseScore(Long userId, int points, String reason) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found : " + userId));

        int newScore = Math.max(0,user.getTrustScore() - points);
        user.setTrustScore(newScore);
        userRepository.save(user);

        log.info("Trust score decreased for user {} by {} points. Reason: {}. New score: {}",
                userId, points, reason, newScore);

    }

    @Override
    public int getCurrentScore(Long userId) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found : " + userId));

        return user.getTrustScore();
    }

    @Override
    public String getScoreCategory(int score) {
        if (score >= BankConfig.TRUST_SCORE_PREMIUM) return "PREMIUM";
        if (score >= BankConfig.TRUST_SCORE_GOOD) return "GOOD";
        if (score >= BankConfig.TRUST_SCORE_AVERAGE) return "AVERAGE";
        if (score >= BankConfig.TRUST_SCORE_RISKY) return "RISKY";
        return "HIGH_RISK";
    }

    @Override
    public boolean isEligibleForLoan(Long userId) {
        int score = getCurrentScore(userId);
        return score >= BankConfig.TRUST_SCORE_RISKY;
    }

}
