package com.BankingSystem.service.user.impl;

import com.BankingSystem.BankConfig;
import com.BankingSystem.dto.request.UserRegistrationRequest;
import com.BankingSystem.dto.response.UserResponse;
import com.BankingSystem.entity.notification.NotificationPreference;
import com.BankingSystem.entity.users.Role;
import com.BankingSystem.entity.users.User;
import com.BankingSystem.exception.DuplicateResourceException;
import com.BankingSystem.exception.ResourceNotFoundException;
import com.BankingSystem.repo.NotificationPreferenceRepository;
import com.BankingSystem.repo.UserRepository;
import com.BankingSystem.service.trust.TrustScoreService;
import com.BankingSystem.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final TrustScoreService trustScoreService;

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
                .dateOfBirth(request.getDateOfBirth())
                .annualIncome(request.getAnnualIncome())
                .profession(request.getProfession())
                .address(request.getAddress())
                .role(Role.CUSTOMER)
                .trustScore(BankConfig.INITIAL_TRUST_SCORE)
                .build();

        User savedUser = userRepository.save(user);

        NotificationPreference preference = NotificationPreference.builder()
                .user(savedUser)
                .emailEnabled(true)
                .smsEnabled(true)
                .telegramEnabled(true)
                .build();

        notificationPreferenceRepository.save(preference);

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
        int age = user.getDateOfBirth() != null
                ? java.time.Period.between(user.getDateOfBirth(),
                java.time.LocalDate.now()).getYears()
                : 0;

        String category = trustScoreService.getScoreCategory(user.getTrustScore());

        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .dateOfBirth(user.getDateOfBirth())
                .age(age)
                .annualIncome(user.getAnnualIncome())
                .trustScore(user.getTrustScore())
                .trustScoreCategory(category)
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}