package com.BankingSystem.util;

import com.BankingSystem.entity.bank.Branch;
import com.BankingSystem.repo.BranchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final BranchRepository branchRepository;

    @Override
    public void run(String... args) {
        if (branchRepository.count() == 0) {
            seedBranches();
            log.info("Branch data seeded successfully.");
        }
    }

    private void seedBranches() {
        branchRepository.save(Branch.builder()
                .branchCode("IND001")
                .branchName("Indore Main Branch")
                .city("Indore")
                .state("Madhya Pradesh")
                .address("MG Road, Indore, MP 452001")
                .ifscCode("AKBK0IND001")
                .contactNumber("0731-1234567")
                .isActive(true)
                .build());

        branchRepository.save(Branch.builder()
                .branchCode("MUM001")
                .branchName("Mumbai Central Branch")
                .city("Mumbai")
                .state("Maharashtra")
                .address("Nariman Point, Mumbai, MH 400021")
                .ifscCode("AKBK0MUM001")
                .contactNumber("022-1234567")
                .isActive(true)
                .build());

        branchRepository.save(Branch.builder()
                .branchCode("DEL001")
                .branchName("Delhi Connaught Place Branch")
                .city("New Delhi")
                .state("Delhi")
                .address("Connaught Place, New Delhi 110001")
                .ifscCode("AKBK0DEL001")
                .contactNumber("011-1234567")
                .isActive(true)
                .build());

        branchRepository.save(Branch.builder()
                .branchCode("BLR001")
                .branchName("Bangalore MG Road Branch")
                .city("Bangalore")
                .state("Karnataka")
                .address("MG Road, Bangalore, KA 560001")
                .ifscCode("AKBK0BLR001")
                .contactNumber("080-1234567")
                .isActive(true)
                .build());
    }
}