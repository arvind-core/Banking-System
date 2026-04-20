package com.BankingSystem.service.user.impl;

import com.BankingSystem.dto.request.UserRegistrationRequest;
import com.BankingSystem.dto.response.UserResponse;
import com.BankingSystem.entity.users.Role;
import com.BankingSystem.entity.users.User;
import com.BankingSystem.exception.DuplicateResourceException;
import com.BankingSystem.exception.ResourceNotFoundException;
import com.BankingSystem.repo.UserRepository;
import com.BankingSystem.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse registerUser(UserRegistrationRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
             throw new DuplicateResourceException("Email already registered");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new DuplicateResourceException("Phone number already registered");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .role(Role.CUSTOMER)
                .build();

        User savedUser = userRepository.save(user);

        return mapToUserResponse(savedUser);
    }

    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        return mapToUserResponse(user);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return mapToUserResponse(user);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}