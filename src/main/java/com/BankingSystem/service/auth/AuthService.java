package com.BankingSystem.service.auth;

import com.BankingSystem.dto.request.LoginRequest;
import com.BankingSystem.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
}
