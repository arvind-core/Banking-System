package com.BankingSystem.service.user;

import com.BankingSystem.dto.request.UserRegistrationRequest;
import com.BankingSystem.dto.response.UserResponse;

public interface UserService {

    UserResponse registerUser(UserRegistrationRequest request);

    UserResponse getUserById(Long id);

    UserResponse getUserByEmail(String email);
}