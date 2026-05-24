package com.BankingSystem.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BranchCreateRequest {

    @NotBlank(message = "Branch code is required")
    private String branchCode;

    @NotBlank(message = "Branch name is required")
    private String branchName;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "IFSC code is required")
    private String ifscCode;

    @NotBlank(message = "Contact number is required")
    private String contactNumber;
}
