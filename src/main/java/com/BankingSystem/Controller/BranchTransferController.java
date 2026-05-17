package com.BankingSystem.Controller;

import com.BankingSystem.dto.request.branchTransfer.BranchTransferCreate;
import com.BankingSystem.dto.request.branchTransfer.BranchTransferReviewRequest;
import com.BankingSystem.dto.response.branchTransfer.BranchTransferResponse;
import com.BankingSystem.service.branchTransfer.BranchTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/branch-transfers")
@RequiredArgsConstructor
public class BranchTransferController {

    private final BranchTransferService branchTransferService;

    @PostMapping("/request/{userId}")
    public ResponseEntity<BranchTransferResponse> requestTransfer(
            @Valid @RequestBody BranchTransferCreate request,
            @PathVariable Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(branchTransferService.requestTransfer(request, userId));
    }

    @PatchMapping("/current-branch-review")
    public ResponseEntity<BranchTransferResponse> currentBranchReview(
            @Valid @RequestBody BranchTransferReviewRequest request) {
        return ResponseEntity.ok(
                branchTransferService.currentBranchReview(request));
    }

    @PatchMapping("/new-branch-review")
    public ResponseEntity<BranchTransferResponse> newBranchReview(
            @Valid @RequestBody BranchTransferReviewRequest request) {
        return ResponseEntity.ok(
                branchTransferService.newBranchReview(request));
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<List<BranchTransferResponse>> getHistory(
            @PathVariable Long userId) {
        return ResponseEntity.ok(
                branchTransferService.getUserTransferHistory(userId));
    }

    @GetMapping("/pending/current-branch/{branchId}")
    public ResponseEntity<List<BranchTransferResponse>>
    getPendingForCurrentBranch(@PathVariable Long branchId) {
        return ResponseEntity.ok(
                branchTransferService.getPendingForCurrentBranch(branchId));
    }

    @GetMapping("/pending/new-branch/{branchId}")
    public ResponseEntity<List<BranchTransferResponse>>
    getPendingForNewBranch(@PathVariable Long branchId) {
        return ResponseEntity.ok(
                branchTransferService.getPendingForNewBranch(branchId));
    }
}