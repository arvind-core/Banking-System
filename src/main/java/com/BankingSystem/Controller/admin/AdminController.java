package com.BankingSystem.Controller.admin;

import com.BankingSystem.dto.response.BankLedgerResponse;
import com.BankingSystem.service.bank.BankLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final BankLedgerService bankLedgerService;

    @GetMapping("/ledger")
    public ResponseEntity<BankLedgerResponse>  getBankLedger(){
        return ResponseEntity.ok(bankLedgerService.getLedgerStatus());
    }
}
