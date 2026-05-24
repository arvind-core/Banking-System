package com.BankingSystem.service.admin;

import com.BankingSystem.dto.request.admin.AssignManagerRequest;
import com.BankingSystem.dto.request.admin.BranchCreateRequest;
import com.BankingSystem.dto.response.UserResponse;
import com.BankingSystem.dto.response.admin.BranchResponse;
import com.BankingSystem.dto.response.admin.SystemDashboardResponse;

import java.util.List;

public interface AdminService {

    BranchResponse createBranch(BranchCreateRequest request);

    BranchResponse deactivateBranch(Long branchId);

    BranchResponse activateBranch(Long branchId);

    List<BranchResponse> getAllBranches();

    BranchResponse assignManager(AssignManagerRequest request);

    BranchResponse revokeManager(Long branchId);

    BranchResponse transferManager(Long managerId, Long newBranchId);

    UserResponse suspendUser(Long userId);

    UserResponse activateUser(Long userId);

    UserResponse promoteToAdmin(Long userId);

    List<UserResponse> getAllManagers();

    List<UserResponse> getAllUsers();

    SystemDashboardResponse getSystemDashboard();
}
