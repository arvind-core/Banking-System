package com.BankingSystem.dto.response;

import com.BankingSystem.entity.users.Role;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String address;
    private Role role;
    private LocalDateTime createdAt;
    private LocalDate dateOfBirth;
    private Integer age;
    private BigDecimal annualIncome;
    private Integer trustScore;
    private String trustScoreCategory;

}