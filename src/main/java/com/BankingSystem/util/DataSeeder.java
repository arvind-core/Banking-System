package com.BankingSystem.util;

import com.BankingSystem.entity.bank.BankLedger;
import com.BankingSystem.entity.bank.Branch;
import com.BankingSystem.entity.notification.NotificationPreference;
import com.BankingSystem.entity.users.Profession;
import com.BankingSystem.entity.users.Role;
import com.BankingSystem.entity.users.User;
import com.BankingSystem.repo.BankLedgerRepository;
import com.BankingSystem.repo.BranchRepository;
import com.BankingSystem.repo.NotificationPreferenceRepository;
import com.BankingSystem.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final BranchRepository branchRepository;
    private final BankLedgerRepository bankLedgerRepository;
    private final UserRepository userRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedBranches();
        seedBankLedger();
        seedAdminUser();
    }

    private void seedBranches() {
        if (branchRepository.count() > 0) return;

        branchRepository.saveAll(List.of(
                Branch.builder()
                        .branchCode("IND001")
                        .branchName("Indore Main Branch")
                        .city("Indore")
                        .state("Madhya Pradesh")
                        .address("MG Road, Indore, MP 452001")
                        .ifscCode("AKBK0IND001")
                        .contactNumber("0731-1234567")
                        .isActive(true)
                        .build(),

                Branch.builder()
                        .branchCode("MUM001")
                        .branchName("Mumbai Central Branch")
                        .city("Mumbai")
                        .state("Maharashtra")
                        .address("Nariman Point, Mumbai, MH 400021")
                        .ifscCode("AKBK0MUM001")
                        .contactNumber("022-1234567")
                        .isActive(true)
                        .build(),

                Branch.builder()
                        .branchCode("DEL001")
                        .branchName("Delhi Connaught Place Branch")
                        .city("New Delhi")
                        .state("Delhi")
                        .address("Connaught Place, New Delhi 110001")
                        .ifscCode("AKBK0DEL001")
                        .contactNumber("011-1234567")
                        .isActive(true)
                        .build(),

                Branch.builder()
                        .branchCode("BLR001")
                        .branchName("Bangalore MG Road Branch")
                        .city("Bangalore")
                        .state("Karnataka")
                        .address("MG Road, Bangalore, KA 560001")
                        .ifscCode("AKBK0BLR001")
                        .contactNumber("080-1234567")
                        .isActive(true)
                        .build(),

                Branch.builder()
                        .branchCode("CHN001")
                        .branchName("Chennai Anna Salai Branch")
                        .city("Chennai")
                        .state("Tamil Nadu")
                        .address("Anna Salai, Chennai, TN 600002")
                        .ifscCode("AKBK0CHN001")
                        .contactNumber("044-1234567")
                        .isActive(true)
                        .build()
        ));

        System.out.println("✓ Branches seeded successfully.");
    }

    private void seedBankLedger() {
        if (bankLedgerRepository.count() > 0) return;

        bankLedgerRepository.save(BankLedger.builder()
                .totalCapital(new BigDecimal("100000000"))
                .totalDeposits(BigDecimal.ZERO)
                .totalLoanBook(BigDecimal.ZERO)
                .totalCreditExposure(BigDecimal.ZERO)
                .totalReserve(new BigDecimal("20000000"))
                .build());

        System.out.println("✓ Bank ledger initialized with ₹10 crore capital.");
    }

    private void seedAdminUser() {
        if (!userRepository.findAllByRole(Role.ADMIN).isEmpty()) return;

        String adminEmail = "admin@akashbank.com";
        String defaultPassword = "Admin@123456";

        User admin = User.builder()
                .firstName("System")
                .lastName("Administrator")
                .email(adminEmail)
                .password(passwordEncoder.encode(defaultPassword))
                .phoneNumber("9000000000")
                .address("Head Office, India")
                .profession(Profession.OTHER)
                .role(Role.ADMIN)
                .trustScore(100)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .annualIncome(BigDecimal.valueOf(1000000))
                .isActive(true)
                .build();

        User savedAdmin = userRepository.save(admin);

        NotificationPreference pref = NotificationPreference.builder()
                .user(savedAdmin)
                .emailEnabled(true)
                .smsEnabled(true)
                .telegramEnabled(false)
                .build();

        notificationPreferenceRepository.save(pref);

        System.out.println("\n╔══════════════════════════════════════════╗");
        System.out.println("║         ADMIN USER CREATED               ║");
        System.out.println("║  Email    : " + adminEmail + "      ║");
        System.out.println("║  Password : " + defaultPassword + "            ║");
        System.out.println("║  CHANGE PASSWORD IMMEDIATELY IN PROD     ║");
        System.out.println("╚══════════════════════════════════════════╝\n");
    }
}