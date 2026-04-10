package com.BankingSystem.service;

import com.BankingSystem.dto.request.LoginRequest;
import com.BankingSystem.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
}