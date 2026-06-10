package com.BankingSystem.service.auth;

import com.BankingSystem.dto.request.LoginRequest;
import com.BankingSystem.dto.request.password.ForgotPasswordRequest;
import com.BankingSystem.dto.request.password.ResetPasswordRequest;
import com.BankingSystem.dto.response.AuthResponse;
import com.BankingSystem.entity.users.User;
import com.BankingSystem.exception.InvalidOperationException;
import com.BankingSystem.exception.ResourceNotFoundException;
import com.BankingSystem.repo.UserRepository;
import com.BankingSystem.security.JwtService;
import com.BankingSystem.service.OTPs.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;


    @Override
    public AuthResponse login(LoginRequest request) {

        // This line does everything:
        // 1. Loads User by email via CustomUserDetailsService
        // 2. Check password against Bcrypt hash
        // 3. Throws BadCredentialException if wrong

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Authentication passed - load user and generate token
        User user = userRepository.findByEmailAndIsActiveTrue(request.getEmail()).orElseThrow(() -> new ResourceNotFoundException("User not found with email " + request.getEmail()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());

        String token = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .userId(user.getId())
                .build();
    }

    @Override
    public String forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmailAndIsActiveTrue(request.getEmail()).orElseThrow(() -> new ResourceNotFoundException("User not found with email " + request.getEmail()));

        // User existing OTP service
        return otpService.generatedAndSendOtp(user.getId(), "FORGOT_PASSWORD");
    }

    @Override
    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmailAndIsActiveTrue(request.getEmail()).orElseThrow(() -> new ResourceNotFoundException("User not found with email " + request.getEmail()));

        boolean valid = otpService.verifyOtp(user.getId(), "FORGOT_PASSWORD", request.getOtp());

        if (!valid) {
            throw new InvalidOperationException("Invalid or expired OTP");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return "Password reset successfully. Please login with your new password";
    }
}