package com.BankingSystem.Controller;

import com.BankingSystem.dto.request.UserRegistrationRequest;
import com.BankingSystem.dto.response.UserResponse;
import com.BankingSystem.exception.InvalidOperationException;
import com.BankingSystem.service.user.UserService;
import com.BankingSystem.service.OTPs.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final OtpService otpService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserRegistrationRequest request) {
        UserResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        UserResponse response = userService.getUserByEmail(email);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{userId}/telegram-chat-id")
    public ResponseEntity<String> updateTelegramChatId(@PathVariable Long userId, @RequestParam String chatId) {
        userService.updateTelegramChatId(userId, chatId);
        return ResponseEntity.ok("Telegram notifications enabled.");
    }

    @PostMapping("/{userId}/otp/generate")
    public ResponseEntity<String> generateOtp(@PathVariable Long userId, @RequestParam String operation) {
        return ResponseEntity.ok(otpService.generatedAndSendOtp(userId, operation));
    }

    @PostMapping("/{userId}/otp/verify")
    public ResponseEntity<Boolean> verifyOtp(@PathVariable Long userId, @RequestParam String operation, @RequestParam String otp) {
        boolean valid = otpService.verifyOtp(userId, operation, otp);
        if(!valid) {
            throw new InvalidOperationException("Invalid or expired OTP.");
        }
        return ResponseEntity.ok(true);
    }
}