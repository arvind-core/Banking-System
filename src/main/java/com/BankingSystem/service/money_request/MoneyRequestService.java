package com.BankingSystem.service.money_request;

import com.BankingSystem.dto.request.moeny_request.MoneyRequestCreate;
import com.BankingSystem.dto.response.money_request_response.MoneyRequestResponse;

import java.util.List;

public interface MoneyRequestService {

    MoneyRequestResponse createRequest(MoneyRequestCreate request, Long requesterId);

    MoneyRequestResponse acceptRequest(String requestReference, Long payerUserId);

    MoneyRequestResponse rejectRequest(String requestReference, Long payerUserId);

    List<MoneyRequestResponse> getIncomingRequests(Long userId);

    List<MoneyRequestResponse> getSentRequests(Long userId);

    void expireOldRequests();
}
