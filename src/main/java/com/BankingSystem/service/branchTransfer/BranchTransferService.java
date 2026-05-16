package com.BankingSystem.service.branchTransfer;

import com.BankingSystem.dto.request.branchTransfer.BranchTransferReviewRequest;
import com.BankingSystem.dto.response.branchTransfer.BranchTransferResponse;
import com.BankingSystem.entity.bank.BranchTransferRequest;

import java.util.List;

public interface BranchTransferService {

    BranchTransferResponse requestTransfer(BranchTransferRequest request, Long userId);

    BranchTransferResponse currentBranchReview(BranchTransferReviewRequest request);

    BranchTransferResponse newBranchReview(BranchTransferReviewRequest request);

    List<BranchTransferResponse> getUserTransferHistory(Long userId);

    List<BranchTransferResponse> getPendingForCurrentBranch(Long branchId);

    List<BranchTransferResponse> getPendingForNewBranch(Long branchId);
}
