package com.BankingSystem.Controller;

import com.BankingSystem.dto.response.UserResponse;
import com.BankingSystem.dto.response.managerDashoboard.BranchMetricsResponse;
import com.BankingSystem.dto.response.managerDashoboard.ManagerDashboardResponse;
import com.BankingSystem.service.manager.ManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
public class ManagerController {
    private final ManagerService managerService;

    // Dashboard - all Pending items + branch metrics in one call
    @GetMapping("/dashboard/{branchId}")
    public ResponseEntity<ManagerDashboardResponse> getDashboard(@PathVariable Long branchId){
        return ResponseEntity.ok(managerService.getDashboard(branchId));
    }

    // Detailed branch metrics
    @GetMapping("/branch/{branchId}/metrics")
    public ResponseEntity<BranchMetricsResponse> getBranchMetrics(@PathVariable Long branchId){
        return ResponseEntity.ok(managerService.getBranchMetrics(branchId));
    }

    // All Customers in branch
    @GetMapping("/branch/{branchId}/customers")
    public ResponseEntity<List<UserResponse>> getCustomers(@PathVariable Long branchId){
        return ResponseEntity.ok(managerService.getCustomersInBranch(branchId));
    }

    // Individual customer full profile
    @GetMapping("/customer/{userId}")
    public ResponseEntity<UserResponse> getCustomerDetails(@PathVariable Long userId){
        return ResponseEntity.ok(managerService.getCustomerDetails(userId));
    }
}

















