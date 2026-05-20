package com.BankingSystem.service.manager;

import com.BankingSystem.dto.response.UserResponse;
import com.BankingSystem.dto.response.managerDashoboard.BranchMetricsResponse;
import com.BankingSystem.dto.response.managerDashoboard.ManagerDashboardResponse;

import java.util.List;

public interface ManagerService {

    ManagerDashboardResponse getDashboard(Long branchId);

    BranchMetricsResponse getBranchMetrics(Long branchId);

    List<UserResponse> getCustomersInBranch(Long branchId);

    UserResponse getCustomerDetails(Long userId);
}
