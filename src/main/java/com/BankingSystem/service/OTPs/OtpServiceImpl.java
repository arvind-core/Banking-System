package com.BankingSystem.service.OTPs;

import com.BankingSystem.BankConfig;
import com.BankingSystem.entity.users.User;
import com.BankingSystem.exception.InvalidOperationException;
import com.BankingSystem.exception.ResourceNotFoundException;
import com.BankingSystem.repo.UserRepository;
import com.BankingSystem.util.notifications.NotificationEvent;
import com.BankingSystem.util.notifications.NotificationEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService{
    
    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    // Key format: OTP:{userId}:{operation}
    private static final String OTP_KEY_PREFIX = "OTP:";
    private static final String ATTEMPT_KEY_PREFIX = "OTP_ATTEMPTS:";

    @Override
    public String generatedAndSendOtp(Long userId, String operation) {

        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found :" + userId));

        // Check if max attempts exceeded
        String attemptKey = ATTEMPT_KEY_PREFIX + userId + ":" + operation;
        String attemptsStr = redisTemplate.opsForValue().get(attemptKey);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;

        if(attempts >= BankConfig.MAX_OTP_ATTEMPTS){
            throw new InvalidOperationException("Maximum OTP attempts exceeded. " +
                    "Please wait before requesting again.");
        }

        // Generate 6-digit OTP using SecureRandom - cryptographically secure
        String otp = generateSecureOtp();

        // Store in Redis with TTL
        String otpKey = OTP_KEY_PREFIX + userId + ":" + operation;
        redisTemplate.opsForValue().set(otpKey, otp, Duration.ofSeconds(BankConfig.OTP_EXPIRY_SECONDS));

        // Send OTP via all enabled channels
        Map<String , Object> data = new HashMap<>();
        data.put("otp", otp);
        data.put("operation", operation.replace("_", " ").toLowerCase());
        data.put("expirySeconds", BankConfig.OTP_EXPIRY_SECONDS);


        eventPublisher.publishEvent(NotificationEvent.forUser(this,
                NotificationEventType.LOGIN_SUCCESSFUL,          // Using as OTP placeholder
                user.getFirstName() + " " + user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                data));

        log.info("OTP sent to {}, userId : {}", user.getFirstName() + " " + user.getLastName(), userId);

        return "OTP sent to your registered email, SMS, and Telegram";

    }

    @Override
    public boolean verifyOtp(Long userId, String operation, String otp) {

        String otpKey = OTP_KEY_PREFIX + userId + ":" + operation;
        String storeOtp = redisTemplate.opsForValue().get(otpKey);

        if(storeOtp == null){
            log.warn("OTP not found or expired for user {} operation: {}", userId, operation);
            return false;
        }

        if(storeOtp.equals(otp)){
            // Delete OTP immediately after successful verification - one - time use

            invalidOtp(userId, operation);
            log.info("OTP verified successfully for user {} operation: {}",userId, operation);
            return true;
        }
        log.warn("OTP mismatch for user {} operation: {}", userId, operation);
        return false;
    }

    @Override
    public void invalidOtp(Long userId, String operation) {

        String otpKey = OTP_KEY_PREFIX + userId + ":" + operation;
        redisTemplate.delete(otpKey);
    }

    private String generateSecureOtp(){
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}

















