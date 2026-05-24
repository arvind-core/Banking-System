package com.BankingSystem.dto.response.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BranchResponse {

    private Long id;
    private String branchCode;
    private String branchName;
    private String city;
    private String state;
    private String address;
    private String ifscCode;
    private String contactNumber;
    private boolean isActive;
    private String assignedManagerName;
    private Long assignedManagerId;
}