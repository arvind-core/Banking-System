package com.BankingSystem.service.branchTransfer;

import com.BankingSystem.dto.request.branchTransfer.BranchTransferCreate;
import com.BankingSystem.dto.request.branchTransfer.BranchTransferReviewRequest;
import com.BankingSystem.dto.response.branchTransfer.BranchTransferResponse;

import java.util.List;

public interface BranchTransferService {

    BranchTransferResponse requestTransfer(BranchTransferCreate request, Long userId);

    BranchTransferResponse currentBranchReview(BranchTransferReviewRequest request);

    BranchTransferResponse newBranchReview(BranchTransferReviewRequest request);

    List<BranchTransferResponse> getUserTransferHistory(Long userId);

    List<BranchTransferResponse> getPendingForCurrentBranch(Long branchId);

    List<BranchTransferResponse> getPendingForNewBranch(Long branchId);
}
