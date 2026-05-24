package com.BankingSystem.service.admin;

import com.BankingSystem.dto.request.admin.AssignManagerRequest;
import com.BankingSystem.dto.request.admin.BranchCreateRequest;
import com.BankingSystem.dto.response.UserResponse;
import com.BankingSystem.dto.response.admin.BranchResponse;
import com.BankingSystem.dto.response.admin.SystemDashboardResponse;
import com.BankingSystem.entity.bank.Branch;
import com.BankingSystem.entity.notification.InAppNotificationType;
import com.BankingSystem.entity.users.Role;
import com.BankingSystem.entity.users.User;
import com.BankingSystem.exception.DuplicateResourceException;
import com.BankingSystem.exception.InvalidOperationException;
import com.BankingSystem.exception.ResourceNotFoundException;
import com.BankingSystem.repo.*;
import com.BankingSystem.service.bank.BankLedgerService;
import com.BankingSystem.service.inAppNotifications.NotificationPanelService;
import com.BankingSystem.service.trust.TrustScoreService;
import com.BankingSystem.util.notifications.NotificationEvent;
import com.BankingSystem.util.notifications.NotificationEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final CreditCardRepository creditCardRepository;
    private final DebitCardRepository debitCardRepository;
    private final BankLedgerService bankLedgerService;
    private final NotificationPanelService notificationPanelService;
    private final TrustScoreService trustScoreService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public BranchResponse createBranch(BranchCreateRequest request){

        if(branchRepository.existsByBranchCode(request.getBranchCode())){
            throw new DuplicateResourceException("Branch with code " + request.getBranchCode() + " already exists");
        }

        if (branchRepository.existsByIfscCode(request.getIfscCode())){
            throw new DuplicateResourceException("Branch with IFSC  " + request.getIfscCode() + " already exists");
        }

        Branch branch = Branch.builder()
                .branchCode(request.getBranchCode())
                .branchName(request.getBranchName())
                .city(request.getCity())
                .state(request.getState())
                .address(request.getAddress())
                .ifscCode(request.getIfscCode())
                .contactNumber(request.getContactNumber())
                .isActive(true)
                .build();

        return mapToBranchResponse(branchRepository.save(branch));
    }

    @Override
    @Transactional
    public BranchResponse deactivateBranch(Long branchId){
        Branch branch = branchRepository.findById(branchId).orElseThrow(() -> new ResourceNotFoundException("Branch not found : " + branchId));

        if (!branch.isActive()){
            throw new InvalidOperationException("Branch is already inactive.");
        }

        long activeAccounts = accountRepository.findByBranchAndIsActiveTrue(branch).size();
        if (activeAccounts > 0){
            throw new InvalidOperationException("Can not deactivate branch with " + activeAccounts + " active accounts. Transfer accounts first");
        }
        branch.setActive(false);
        return mapToBranchResponse(branchRepository.save(branch));
    }

    @Override
    @Transactional
    public BranchResponse activateBranch(Long branchId) {
        Branch branch = branchRepository.findById(branchId).orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));
        branch.setActive(true);
        return mapToBranchResponse(branchRepository.save(branch));
    }

    @Override
    public List<BranchResponse> getAllBranches() {
        return branchRepository.findAll()
                .stream()
                .map(this::mapToBranchResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BranchResponse assignManager(AssignManagerRequest request) {
        User user = userRepository.findById(request.getUserId()).orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUserId()));

        Branch branch = branchRepository.findById(request.getBranchId()).orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + request.getBranchId()));

        if (!branch.isActive()) {
            throw new InvalidOperationException("Cannot assign manager to inactive branch.");
        }

        // Promote user to BRANCH_MANAGER role if not already
        user.setRole(Role.BRANCH_MANAGER);
        userRepository.save(user);

        // Assign to branch
        branch.setAssignedManager(user);
        Branch saved = branchRepository.save(branch);

        // In-app notification to the newly assigned manager
        notificationPanelService.sendToUser(
                user.getId(),
                "Branch Manager Assignment",
                "You have been assigned as Branch Manager of " +
                        branch.getBranchName() + ", " + branch.getCity() + ".",
                InAppNotificationType.MANAGER_ASSIGNED,
                branch.getId(),
                "BRANCH");

        // External notification
        Map<String, Object> data = new HashMap<>();
        data.put("branchName", branch.getBranchName());
        data.put("branchCode", branch.getBranchCode());
        data.put("city", branch.getCity());

        eventPublisher.publishEvent(NotificationEvent.forUser(this,
                NotificationEventType.LOGIN_SUCCESSFUL,
                user.getFirstName() + " " + user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                data));

        return mapToBranchResponse(saved);
    }

    @Override
    @Transactional
    public BranchResponse revokeManager(Long branchId) {
        Branch branch = branchRepository.findById(branchId).orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));

        if (branch.getAssignedManager() == null) {
            throw new InvalidOperationException("Branch has no assigned manager.");
        }

        User manager = branch.getAssignedManager();

        // Revert role to CUSTOMER
        manager.setRole(Role.CUSTOMER);
        userRepository.save(manager);

        // Remove from branch
        branch.setAssignedManager(null);
        Branch saved = branchRepository.save(branch);

        // Notify the manager
        notificationPanelService.sendToUser(
                manager.getId(),
                "Manager Role Revoked",
                "Your Branch Manager role for " +
                        branch.getBranchName() + " has been revoked.",
                InAppNotificationType.MANAGER_REVOKED,
                branch.getId(),
                "BRANCH");

        return mapToBranchResponse(saved);
    }

    @Override
    @Transactional
    public BranchResponse transferManager(Long managerId, Long newBranchId) {

        User manager = userRepository.findById(managerId).orElseThrow(() -> new ResourceNotFoundException("Manager not found: " + managerId));

        if (manager.getRole() != Role.BRANCH_MANAGER) {
            throw new InvalidOperationException("User is not a branch manager.");
        }

        Branch newBranch = branchRepository.findById(newBranchId).orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + newBranchId));

        if (!newBranch.isActive()) {
            throw new InvalidOperationException("Cannot transfer to inactive branch.");
        }

        // Find current branch and remove manager
        branchRepository.findAll()
                .stream()
                .filter(b -> b.getAssignedManager() != null && b.getAssignedManager().getId().equals(managerId))
                .findFirst()
                .ifPresent(currentBranch -> {currentBranch.setAssignedManager(null);branchRepository.save(currentBranch);
                });

        // Assign to new branch
        newBranch.setAssignedManager(manager);
        Branch saved = branchRepository.save(newBranch);

        // Notify manager
        notificationPanelService.sendToUser(
                managerId,
                "Branch Transfer",
                "You have been transferred to " +
                        newBranch.getBranchName() + ", " +
                        newBranch.getCity() + ".",
                InAppNotificationType.MANAGER_TRANSFERRED,
                newBranch.getId(),
                "BRANCH");

        return mapToBranchResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse suspendUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (user.getRole() == Role.ADMIN) {
            throw new InvalidOperationException("Cannot suspend an admin user.");
        }

        user.setActive(false);
        return mapToUserResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse activateUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setActive(true);
        return mapToUserResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse promoteToAdmin(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setRole(Role.ADMIN);
        return mapToUserResponse(userRepository.save(user));
    }

    @Override
    public List<UserResponse> getAllManagers() {
        return userRepository.findAllByRole(Role.BRANCH_MANAGER)
                .stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SystemDashboardResponse getSystemDashboard() {
        long totalBranches = branchRepository.count();
        long activeBranches = branchRepository.findByIsActiveTrue().size();
        long totalUsers = userRepository.count();
        long totalManagers = userRepository.findAllByRole(Role.BRANCH_MANAGER).size();
        long totalCustomers = userRepository.findAllByRole(Role.CUSTOMER).size();
        long totalAccounts = accountRepository.count();
        long totalActiveLoans = loanAccountRepository.findAllActiveLoans().size();
        long totalCreditCards = creditCardRepository.count();
        long totalDebitCards = debitCardRepository.count();

        var ledger = bankLedgerService.getLedgerStatus();

        return SystemDashboardResponse.builder()
                .totalBranches((int) totalBranches)
                .activeBranches((int) activeBranches)
                .totalUsers((int) totalUsers)
                .totalManagers((int) totalManagers)
                .totalCustomers((int) totalCustomers)
                .totalAccounts(totalAccounts)
                .totalActiveLoans(totalActiveLoans)
                .totalCreditCards(totalCreditCards)
                .totalDebitCards(totalDebitCards)
                .totalSystemDeposits(ledger.getTotalDeposits())
                .totalLoanBook(ledger.getTotalLoanBook())
                .totalCreditExposure(ledger.getTotalCreditExposure())
                .availableLendingCapacity(ledger.getAvailableLendingCapacity())
                .bankLedger(ledger)
                .build();
    }

    private BranchResponse mapToBranchResponse(Branch branch) {
        String managerName = branch.getAssignedManager() != null ? branch.getAssignedManager().getFirstName() + " " + branch.getAssignedManager().getLastName() : null;

        Long managerId = branch.getAssignedManager() != null ? branch.getAssignedManager().getId() : null;

        return BranchResponse.builder()
                .id(branch.getId())
                .branchCode(branch.getBranchCode())
                .branchName(branch.getBranchName())
                .city(branch.getCity())
                .state(branch.getState())
                .address(branch.getAddress())
                .ifscCode(branch.getIfscCode())
                .contactNumber(branch.getContactNumber())
                .isActive(branch.isActive())
                .assignedManagerName(managerName)
                .assignedManagerId(managerId)
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        int age = user.getDateOfBirth() != null ? Period.between(user.getDateOfBirth(), LocalDate.now()).getYears() : 0;
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .dateOfBirth(user.getDateOfBirth())
                .age(age)
                .annualIncome(user.getAnnualIncome())
                .trustScore(user.getTrustScore())
                .trustScoreCategory(trustScoreService.getScoreCategory(user.getTrustScore()))
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}