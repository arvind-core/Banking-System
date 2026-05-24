package com.BankingSystem.dto.request.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignManagerRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Branch ID is required")
    private Long branchId;
}
