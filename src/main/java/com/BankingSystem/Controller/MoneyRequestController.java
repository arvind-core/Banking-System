package com.BankingSystem.Controller;

import com.BankingSystem.dto.request.moeny_request.MoneyRequestCreate;
import com.BankingSystem.dto.response.money_request_response.MoneyRequestResponse;
import com.BankingSystem.service.money_request.MoneyRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/money-requests")
@RequiredArgsConstructor
public class MoneyRequestController {

    private final MoneyRequestService moneyRequestService;

    @PostMapping("/create/{requesterId}")
    public ResponseEntity<MoneyRequestResponse> createRequest(@Valid @RequestBody MoneyRequestCreate request, @PathVariable Long requesterId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(moneyRequestService.createRequest(request,requesterId));
    }

    @PatchMapping("/accept/{requestReference}/{payerUserId}")
    public ResponseEntity<MoneyRequestResponse> acceptRequest(@PathVariable String requestReference, @PathVariable Long payerUserId) {
        return ResponseEntity.ok(moneyRequestService.acceptRequest(requestReference,payerUserId));
    }

    @PatchMapping("/reject/{requestReference}/{payerUserId}")
    public ResponseEntity<MoneyRequestResponse>  rejectRequest(@PathVariable String requestReference, @PathVariable Long payerUserId) {
        return ResponseEntity.ok(moneyRequestService.rejectRequest(requestReference,payerUserId));
    }

    @GetMapping("/incoming/{userId}")
    public ResponseEntity<List<MoneyRequestResponse>> getIncomingRequests(@PathVariable Long userId) {
        return ResponseEntity.ok(moneyRequestService.getIncomingRequests(userId));
    }

    @GetMapping("/sent/{userId}")
    public ResponseEntity<List<MoneyRequestResponse>> getSentRequests(@PathVariable Long userId) {
        return ResponseEntity.ok(moneyRequestService.getSentRequests(userId));
    }





























}
