package com.BankingSystem.Controller.admin;

import com.BankingSystem.dto.request.admin.AssignManagerRequest;
import com.BankingSystem.dto.request.admin.BranchCreateRequest;
import com.BankingSystem.dto.response.BankLedgerResponse;
import com.BankingSystem.dto.response.UserResponse;
import com.BankingSystem.dto.response.admin.BranchResponse;
import com.BankingSystem.dto.response.admin.SystemDashboardResponse;
import com.BankingSystem.service.admin.AdminService;
import com.BankingSystem.service.bank.BankLedgerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final BankLedgerService bankLedgerService;

    // System dashboard
    @GetMapping("/dashboard")
    public ResponseEntity<SystemDashboardResponse> getSystemDashboard() {
        return ResponseEntity.ok(adminService.getSystemDashboard());
    }

    // Bank ledger
    @GetMapping("/ledger")
    public ResponseEntity<BankLedgerResponse> getBankLedger() {
        return ResponseEntity.ok(bankLedgerService.getLedgerStatus());
    }

    // Branch management
    @PostMapping("/branches")
    public ResponseEntity<BranchResponse> createBranch(
            @Valid @RequestBody BranchCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.createBranch(request));
    }

    @GetMapping("/branches")
    public ResponseEntity<List<BranchResponse>> getAllBranches() {
        return ResponseEntity.ok(adminService.getAllBranches());
    }

    @PatchMapping("/branches/{branchId}/deactivate")
    public ResponseEntity<BranchResponse> deactivateBranch(
            @PathVariable Long branchId) {
        return ResponseEntity.ok(adminService.deactivateBranch(branchId));
    }

    @PatchMapping("/branches/{branchId}/activate")
    public ResponseEntity<BranchResponse> activateBranch(
            @PathVariable Long branchId) {
        return ResponseEntity.ok(adminService.activateBranch(branchId));
    }

    // Manager management
    @PostMapping("/managers/assign")
    public ResponseEntity<BranchResponse> assignManager(
            @Valid @RequestBody AssignManagerRequest request) {
        return ResponseEntity.ok(adminService.assignManager(request));
    }

    @PatchMapping("/managers/revoke/{branchId}")
    public ResponseEntity<BranchResponse> revokeManager(
            @PathVariable Long branchId) {
        return ResponseEntity.ok(adminService.revokeManager(branchId));
    }

    @PatchMapping("/managers/{managerId}/transfer/{newBranchId}")
    public ResponseEntity<BranchResponse> transferManager(
            @PathVariable Long managerId,
            @PathVariable Long newBranchId) {
        return ResponseEntity.ok(
                adminService.transferManager(managerId, newBranchId));
    }

    @GetMapping("/managers")
    public ResponseEntity<List<UserResponse>> getAllManagers() {
        return ResponseEntity.ok(adminService.getAllManagers());
    }

    // User management
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PatchMapping("/users/{userId}/suspend")
    public ResponseEntity<UserResponse> suspendUser(
            @PathVariable Long userId) {
        return ResponseEntity.ok(adminService.suspendUser(userId));
    }

    @PatchMapping("/users/{userId}/activate")
    public ResponseEntity<UserResponse> activateUser(
            @PathVariable Long userId) {
        return ResponseEntity.ok(adminService.activateUser(userId));
    }

    @PatchMapping("/users/{userId}/promote-admin")
    public ResponseEntity<UserResponse> promoteToAdmin(
            @PathVariable Long userId) {
        return ResponseEntity.ok(adminService.promoteToAdmin(userId));
    }
}