package com.BankingSystem.service.auth;

import com.BankingSystem.dto.request.LoginRequest;
import com.BankingSystem.dto.request.password.ForgotPasswordRequest;
import com.BankingSystem.dto.request.password.ResetPasswordRequest;
import com.BankingSystem.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);

    String forgotPassword(ForgotPasswordRequest request);
    String resetPassword(ResetPasswordRequest request);
}
