package com.BankingSystem.service.creditcard;

import com.BankingSystem.dto.request.creditcard.CardPaymentRequest;
import com.BankingSystem.dto.request.creditcard.CardTransactionRequest;
import com.BankingSystem.dto.request.creditcard.CreditCardApplicationRequest;
import com.BankingSystem.dto.request.creditcard.LimitIncreaseRequest;
import com.BankingSystem.dto.response.creditcard.BillingCycleResponse;
import com.BankingSystem.dto.response.creditcard.CardTransactionResponse;
import com.BankingSystem.dto.response.creditcard.CreditCardResponse;
import com.BankingSystem.entity.account.Account;
import com.BankingSystem.entity.card.*;
import com.BankingSystem.entity.notification.InAppNotificationType;
import com.BankingSystem.entity.users.User;
import com.BankingSystem.exception.InvalidOperationException;
import com.BankingSystem.exception.ResourceNotFoundException;
import com.BankingSystem.repo.*;
import com.BankingSystem.service.bank.BankLedgerService;
import com.BankingSystem.service.inAppNotifications.NotificationPanelService;
import com.BankingSystem.service.trust.TrustScoreService;
import com.BankingSystem.util.notifications.NotificationEvent;
import com.BankingSystem.util.notifications.NotificationEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

import static com.BankingSystem.BankConfig.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditCardServiceImplementation implements CreditCardService {

    private final CreditCardRepository creditCardRepository;
    private final CardTransactionRepository cardTransactionRepository;
    private final BillingCycleRepository billingCycleRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final BankLedgerService bankLedgerService;
    private final TrustScoreService trustScoreService;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationPanelService notificationPanelService;

    @Override
    @Transactional
    public CreditCardResponse applyForCard(CreditCardApplicationRequest request) {
        Account linkedAccount = accountRepository.findByAccountNumberAndIsActiveTrue(request.getAccountNumber()).orElseThrow(() -> new ResourceNotFoundException("Account not found : " + request.getAccountNumber()));

        User user = linkedAccount.getUser();

        // Age check
        if (user.getDateOfBirth() == null) {
            throw new InvalidOperationException("Date of birth required for credit card application");
        }

        int age = Period.between(user.getDateOfBirth(), LocalDate.now()).getYears();
        if (age < MIN_LOAN_AGE || age > MAX_LOAN_AGE) {
            throw new InvalidOperationException("Age must be between " + MIN_LOAN_AGE + " and " + MAX_LOAN_AGE + "for credit card");
        }

        // Trust Score Eligibility
        int trustScore = user.getTrustScore();
        if (trustScore < MIN_TRUST_SCORE_FOR_CREDIT_CARD) {
            throw new InvalidOperationException("Trust score too low for credit card. " +
                    "Minimum required : " + MIN_TRUST_SCORE_FOR_CREDIT_CARD);
        }

        // Card type eligibility based on trust score
        validateCardTypeEligibility(request.getCardType(), trustScore);

        // Check if user already has this card type
        if (creditCardRepository.existsByUserAndCardType(user, request.getCardType())) {
            throw new InvalidOperationException("You already have an active " + request.getCardType() + " credit card");
        }

        // Annual Income check
        if ((user.getAnnualIncome() == null) || user.getAnnualIncome().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Annual income must be declared before applying and must be greater than zero");
        }

        // Calculate credit limit
        BigDecimal creditLimit = calculateCreditLimit(request.getCardType(), trustScore, user.getAnnualIncome());

        // Check Bank lending capacity
        if (!bankLedgerService.canLend(creditLimit)) {
            throw new InvalidOperationException("Bank does not sufficient fund capacity " +
                    "to issue this credit card at this time");
        }

        // Determine if auto-approve or needs manager approval
        CardStatus initialStatus = request.getCardType() == CardType.SIGNATURE ? CardStatus.PENDING_APPROVAL : CardStatus.ACTIVE;

        String cardNumber = generateCardNumber();
        LocalDate today = LocalDate.now();

        CreditCard creditCard = CreditCard.builder()
                .cardNumber(cardNumber)
                .maskedCardNumber(maskedCardNumber(cardNumber))
                .cardHolderName(user.getFirstName() + " " + user.getLastName())
                .user(user)
                .linkedAccount(linkedAccount)
                .cardType(request.getCardType())
                .status(initialStatus)
                .creditLimit(creditLimit)
                .availableLimit(initialStatus == CardStatus.ACTIVE ? creditLimit : BigDecimal.ZERO)
                .outstandingAmount(BigDecimal.ZERO)
                .minimumDue(BigDecimal.ZERO)
                .totalRewardPoints(BigDecimal.ZERO)
                .expiryDate(today.plusYears(CARD_EXPIRY_YEARS))
                .billingCycleStart(today)
                .nextBillingDate(today.plusMonths(1).withDayOfMonth(1))
                .paymentDueDate(today.plusMonths(1).withDayOfMonth(CREDIT_CARD_PAYMENT_DUE_DAYS))
                .internationalTransactionsEnabled(true)
                .onlineTransactionsEnabled(true)
                .consecutiveMissedPayments(0)
                .build();

        CreditCard saved = creditCardRepository.save(creditCard);

        if (initialStatus == CardStatus.PENDING_APPROVAL) {
            // Find the branch manager of the customer's primary account branch
            accountRepository.findPrimaryAccountByUserPhoneNumber(
                            user.getPhoneNumber())
                    .ifPresent(acc -> {
                        if (acc.getBranch().getAssignedManager() != null) {
                            notificationPanelService.sendToUser(
                                    acc.getBranch().getAssignedManager().getId(),
                                    "Credit Card Approval Required",
                                    saved.getCardHolderName() +
                                            " applied for a SIGNATURE credit card.",
                                    InAppNotificationType
                                            .CREDIT_CARD_APPLICATION_RECEIVED,
                                    saved.getId(),
                                    "CREDIT_CARD");
                        }
                    });
        }

        // If auto-approved, open billing cycle and update ledger
        if (initialStatus == CardStatus.ACTIVE) {
            openNewBillingCycle(saved);
            bankLedgerService.onCreditCardSpend(BigDecimal.ZERO);
        }

        // Notify user
        Map<String, Object> data = new HashMap<>();
        data.put("cardType", saved.getCardType().toString());
        data.put("status", saved.getStatus().toString());
        data.put("creditLimit", saved.getCreditLimit());

        NotificationEventType eventType = initialStatus == CardStatus.ACTIVE ? NotificationEventType.CARD_APPROVED : NotificationEventType.CARD_APPLICATION_SUBMITTED;

        eventPublisher.publishEvent(NotificationEvent.forUser(this,
                eventType,
                user.getFirstName() + " " + user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                data));

        log.info("Credit card application processed for user {}. " +
                        "Type : {}. Status : {}",
                user.getId(), request.getCardType(), initialStatus);

        return mapToCardResponse(saved);
    }

    @Override
    @Transactional
    public CreditCardResponse approveCard(String cardNumber, Long managerId) {
        CreditCard card = creditCardRepository.findByCardNumber(cardNumber).orElseThrow(() -> new ResourceNotFoundException("Credit Card not found : " + cardNumber));

        if (card.getStatus() != CardStatus.PENDING_APPROVAL) {
            throw new InvalidOperationException("Card is not in PENDING APPROVAL state");
        }
        card.setStatus(CardStatus.ACTIVE);
        card.setAvailableLimit(card.getCreditLimit());
        CreditCard saved = creditCardRepository.save(card);

        openNewBillingCycle(saved);

        Map<String, Object> data = new HashMap<>();
        data.put("cardType", saved.getCardType().toString());
        data.put("creditLimit", saved.getCreditLimit());
        data.put("maskedCardNumber", saved.getMaskedCardNumber());

        eventPublisher.publishEvent(NotificationEvent.forUser(this,
                NotificationEventType.CARD_ACTIVATED,
                saved.getCardHolderName(),
                saved.getUser().getEmail(),
                saved.getUser().getPhoneNumber(),
                data));

        return mapToCardResponse(saved);
    }

    @Override
    @Transactional
    public CreditCardResponse rejectCard(String cardNumber, Long managerId, String reason) {
        CreditCard card = creditCardRepository.findByCardNumber(cardNumber).orElseThrow(() -> new ResourceNotFoundException("Credit Card not found : " + cardNumber));

        if (card.getStatus() != CardStatus.PENDING_APPROVAL) {
            throw new InvalidOperationException("Card is not in PENDING APPROVAL state");
        }

        card.setStatus(CardStatus.CANCELLED);
        CreditCard saved = creditCardRepository.save(card);

        Map<String, Object> data = new HashMap<>();
        data.put("cardType", saved.getCardType().toString());
        data.put("reason", reason);

        eventPublisher.publishEvent(NotificationEvent.forUser(this,
                NotificationEventType.CARD_REJECTED,
                saved.getCardHolderName(),
                saved.getUser().getEmail(),
                saved.getUser().getPhoneNumber(),
                data));

        return mapToCardResponse(saved);
    }

    @Override
    @Transactional
    public CardTransactionResponse processTransaction(CardTransactionRequest request) {
        CreditCard card = creditCardRepository.findByCardNumberWithLock(request.getCardNumber()).orElseThrow(() -> new ResourceNotFoundException("Credit Card not found : " + request.getCardNumber()));

        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new InvalidOperationException("Card is not in ACTIVE state, Current state : " + card.getStatus());
        }

        if (card.getAvailableLimit().compareTo(request.getAmount()) < 0) {
            throw new InvalidOperationException("Insufficient credit limit. Available: ₹" + card.getAvailableLimit());
        }

        // Calculate reward points
        BigDecimal rewardPoints = calculateRewardPoints(request.getAmount(), request.getCategory());

        // Update Card
        card.setAvailableLimit(card.getAvailableLimit().subtract(request.getAmount()));
        card.setOutstandingAmount(card.getOutstandingAmount().add(request.getAmount()));
        card.setTotalRewardPoints(card.getTotalRewardPoints().add(rewardPoints));
        creditCardRepository.save(card);

        // Update Billing cycle spend
        BillingCycle currentCycle = billingCycleRepository.findByCreditCardAndStatus(card, BillingStatus.OPEN).orElseGet(() -> openNewBillingCycle(card));

        currentCycle.setTotalSpend(currentCycle.getTotalSpend().add(request.getAmount()));
        currentCycle.setTotalRewardPointsEarned(currentCycle.getTotalRewardPointsEarned().add(rewardPoints));

        BigDecimal newOutstanding = currentCycle.getOpeningOutstanding().add(currentCycle.getTotalSpend());
        currentCycle.setClosingOutstanding(newOutstanding);
        currentCycle.setMinimumDue(calculateMinimumDue(newOutstanding));
        billingCycleRepository.save(currentCycle);

        // Update bank ledger
        bankLedgerService.onCreditCardSpend(request.getAmount());

        // save transaction record
        CardTransaction transaction = CardTransaction.builder()
                .transactionReference("CTX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .creditCard(card)
                .amount(request.getAmount())
                .merchantName(request.getMerchantName())
                .category(request.getCategory())
                .rewardPointsEarned(rewardPoints)
                .availableLimitAfter(card.getAvailableLimit())
                .description(request.getDescription() != null ? request.getDescription() : "Purchase at " + request.getMerchantName())
                .build();

        CardTransaction saved = cardTransactionRepository.save(transaction);

        log.info("Card transaction processed. Card: {}. Amount: ₹{}. " +
                "Merchant: {}.", card.getMaskedCardNumber(), request.getAmount(), request.getMerchantName());
        return mapToTransactionResponse(saved);
    }

    @Override
    @Transactional
    public CreditCardResponse makePayment(CardPaymentRequest request) {

        CreditCard card = creditCardRepository.findByCardNumberWithLock(request.getCardNumber()).orElseThrow(() -> new ResourceNotFoundException("Credit Card not found : " + request.getCardNumber()));

        if (card.getStatus() == CardStatus.CANCELLED) {
            throw new InvalidOperationException("Can not make payment on a cancelled card.");
        }
        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new InvalidOperationException("Can not make payment on a Blocked card.");
        }

        BillingCycle currentCycle = billingCycleRepository.findByCreditCardAndStatus(card, BillingStatus.GENERATED).orElse(billingCycleRepository.findByCreditCardAndStatus(card, BillingStatus.PARTIALLY_PAID).orElseThrow(() -> new InvalidOperationException("No pending bill found for this card")));

        BigDecimal paymentAmount = request.getPaymentAmount();

        if (paymentAmount.compareTo(card.getOutstandingAmount()) > 0) {
            throw new InvalidOperationException("Payment amount ₹" + paymentAmount + " exceeds outstanding amount ₹" + card.getOutstandingAmount());
        }

        // Deduct payment from linked bank account
        Account linkedAccount = accountRepository.findByAccountNumberWithLock(card.getLinkedAccount().getAccountNumber()).orElseThrow(() -> new ResourceNotFoundException("Linked account not found"));

        if (linkedAccount.getBalance().compareTo(request.getPaymentAmount()) < 0) {
            throw new InvalidOperationException(
                    "Insufficient balance in linked account to make payment. " +
                            "Available: ₹" + linkedAccount.getBalance());
        }

        linkedAccount.setBalance(linkedAccount.getBalance().subtract(request.getPaymentAmount()));
        accountRepository.save(linkedAccount);

        // Apply waterfall payment logic
        BigDecimal remaining = applyPayment(card, currentCycle, paymentAmount);

        // Update cycle totals
        currentCycle.setTotalPaid(currentCycle.getTotalPaid().add(paymentAmount));

        // Determine billing status
        if (card.getOutstandingAmount().compareTo(BigDecimal.ZERO) == 0) {
            currentCycle.setStatus(BillingStatus.PAID);
            card.setConsecutiveMissedPayments(0);
            trustScoreService.increaseScore(card.getUser().getId(), SCORE_CREDIT_CARD_FULL_PAYMENT, "Credit card bill paid in full");
        } else {
            currentCycle.setStatus(BillingStatus.PARTIALLY_PAID);
        }

        card.setMinimumDue(calculateMinimumDue(card.getOutstandingAmount()));
        billingCycleRepository.save(currentCycle);
        CreditCard saved = creditCardRepository.save(card);

        // Update bank ledger
        bankLedgerService.onCreditCardPayment(paymentAmount);

        Map<String, Object> data = new HashMap<>();
        data.put("paymentAmount", paymentAmount);
        data.put("outstandingAmount", card.getOutstandingAmount());
        data.put("maskedCardNumber", card.getMaskedCardNumber());

        eventPublisher.publishEvent(NotificationEvent.forUser(this,
                NotificationEventType.CARD_PAYMENT_RECEIVED,
                card.getCardHolderName(),
                card.getUser().getEmail(),
                card.getUser().getPhoneNumber(),
                data));

        return mapToCardResponse(saved);
    }

    @Override
    @Transactional
    public CreditCardResponse blockCard(String cardNumber) {
        CreditCard card = creditCardRepository.findByCardNumber(cardNumber).orElseThrow(() -> new ResourceNotFoundException("Credit Card not found : " + cardNumber));

        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new InvalidOperationException("Only Active cards can be blocked.");
        }

        card.setStatus(CardStatus.BLOCKED);
        CreditCard saved = creditCardRepository.save(card);

        Map<String, Object> data = new HashMap<>();
        data.put("maskedCardNumber", card.getMaskedCardNumber());
        data.put("cardType", card.getCardType());

        eventPublisher.publishEvent(NotificationEvent.forUser(this,
                NotificationEventType.CARD_BLOCKED,
                saved.getCardHolderName(),
                saved.getUser().getEmail(),
                saved.getUser().getPhoneNumber(),
                data));

        return mapToCardResponse(saved);
    }

    @Override
    @Transactional
    public CreditCardResponse unblockCard(String cardNumber, Long managerId) {
        CreditCard card = creditCardRepository.findByCardNumber(cardNumber).orElseThrow(() -> new ResourceNotFoundException("Credit Card not found : " + cardNumber));

        if (card.getStatus() != CardStatus.BLOCKED) {
            throw new InvalidOperationException("Card is not blocked.");
        }

        card.setStatus(CardStatus.ACTIVE);
        CreditCard saved = creditCardRepository.save(card);

        return mapToCardResponse(saved);
    }

    @Override
    @Transactional
    public CreditCardResponse requestLimitIncrease(LimitIncreaseRequest request) {
        CreditCard card = creditCardRepository.findByCardNumber(request.getCardNumber()).orElseThrow(() -> new ResourceNotFoundException("Credit Card not found : " + request.getCardNumber()));

        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new InvalidOperationException("Card must be active for limit increase.");
        }

        User manager = userRepository.findById(request.getReviewedByManagerId()).orElseThrow(() -> new ResourceNotFoundException("Manager not found with id : " + request.getReviewedByManagerId()));

        User user = card.getUser();
        BigDecimal newLimit = request.getRequestedLimit();

        // Validate new limit against income
        BigDecimal maxLimitAllowed = user.getAnnualIncome().divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(CREDIT_LIMIT_INCOME_MULTIPLIER));

        if (newLimit.compareTo(maxLimitAllowed) >= 0) {
            throw new InvalidOperationException("Requested limit ₹" + newLimit +
                    " exceeds maximum allowed ₹" + maxLimitAllowed +
                    " based on your income.");
        }

        if (!bankLedgerService.canLend(newLimit.subtract(card.getCreditLimit()))) {
            throw new InvalidOperationException("Bank can not support this limit increase at this time.");
        }

        BigDecimal difference = newLimit.subtract(card.getCreditLimit());
        card.setCreditLimit(newLimit);
        card.setAvailableLimit(card.getAvailableLimit().add(difference));

        CreditCard saved = creditCardRepository.save(card);

        log.info("Credit limit increased for card {} from ₹{} to ₹{} " +
                "by manager {} ", card.getMaskedCardNumber(), card.getCreditLimit(), newLimit, manager.getId());

        return mapToCardResponse(saved);
    }

    @Override
    public List<CreditCardResponse> getUserCards(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found with id : " + userId));

        return creditCardRepository.findByUser(user)
                .stream()
                .map(this::mapToCardResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CardTransactionResponse> getCardTransactions(String cardNumber) {

        CreditCard card = creditCardRepository.findByCardNumber(cardNumber).orElseThrow(() -> new ResourceNotFoundException("Credit Card not found : " + cardNumber));

        return cardTransactionRepository.
                findByCreditCardOrderByCreatedAtDesc(card)
                .stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BillingCycleResponse> getBillingHistory(String cardNumber) {
        CreditCard card = creditCardRepository.findByCardNumber(cardNumber).orElseThrow(() -> new ResourceNotFoundException("Credit Card not found : " + cardNumber));

        return billingCycleRepository
                .findByCreditCardOrderByCycleStartDateDesc(card)
                .stream()
                .map(this::mapToBillingCycleResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BillingCycleResponse getCurrentBillingCycle(String cardNumber) {
        CreditCard card = creditCardRepository.findByCardNumber(cardNumber).orElseThrow(() -> new ResourceNotFoundException("Credit Card not found : " + cardNumber));

        BillingCycle cycle = billingCycleRepository
                .findByCreditCardAndStatus(card, BillingStatus.OPEN)
                .orElseThrow(() -> new ResourceNotFoundException("No open Billing cycle found."));

        return mapToBillingCycleResponse(cycle);
    }

    private BigDecimal applyPayment(CreditCard card, BillingCycle cycle, BigDecimal paymentAmount) {

        BigDecimal remaining = paymentAmount;

        // step - 1, Clear late payment fee first

        if ((remaining.compareTo(BigDecimal.ZERO) > 0) && (cycle.getLatePaymentFee() != null) && (cycle.getLatePaymentFee().compareTo(BigDecimal.ZERO) > 0)) {
            BigDecimal feeCleared = remaining.min(cycle.getLatePaymentFee());
            cycle.setLatePaymentFee(cycle.getLatePaymentFee().subtract(feeCleared));
            remaining = remaining.subtract(feeCleared);
        }

        // step - 2, clear interest
        if ((remaining.compareTo(BigDecimal.ZERO) > 0) && (cycle.getInterestCharged().compareTo(BigDecimal.ZERO) > 0)) {
            BigDecimal interestCleared = remaining.min(cycle.getInterestCharged());
            cycle.setInterestCharged(cycle.getInterestCharged().subtract(interestCleared));
            remaining = remaining.subtract(interestCleared);
        }

        // step - 3, Apply remainder to principal
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal principalCleared = remaining.min(card.getOutstandingAmount());
            card.setOutstandingAmount(card.getOutstandingAmount().subtract(principalCleared));
            card.setAvailableLimit(card.getAvailableLimit().add(principalCleared));
            remaining = remaining.subtract(principalCleared);
        }
        return remaining;
    }

    private BillingCycle openNewBillingCycle(CreditCard card) {
        LocalDate today = LocalDate.now();
        LocalDate cycleEnd = today.plusDays(1).withDayOfMonth(1).minusDays(1);
        LocalDate dueDate = today.plusMonths(1).withDayOfMonth(CREDIT_CARD_PAYMENT_DUE_DAYS);
        BillingCycle cycle = BillingCycle.builder()
                .creditCard(card)
                .cycleStartDate(today)
                .cycleEndDate(cycleEnd)
                .paymentDueDate(dueDate)
                .totalSpend(BigDecimal.ZERO)
                .minimumDue(BigDecimal.ZERO)
                .openingOutstanding(card.getOutstandingAmount())
                .closingOutstanding(card.getOutstandingAmount())
                .totalPaid(BigDecimal.ZERO)
                .interestCharged(BigDecimal.ZERO)
                .latePaymentFee(BigDecimal.ZERO)
                .totalRewardPointsEarned(BigDecimal.ZERO)
                .daysOverdue(0)
                .status(BillingStatus.OPEN)
                .build();

        return billingCycleRepository.save(cycle);
    }

    private BigDecimal calculateCreditLimit(CardType cardType, int trustScore, BigDecimal annualIncome) {
        double maxByType = switch (cardType) {
            case CLASSIC -> CLASSIC_CARD_MAX_LIMIT;
            case GOLD -> GOLD_CARD_MAX_LIMIT;
            case PLATINUM -> PLATINUM_CARD_MAX_LIMIT;
            case SIGNATURE -> SIGNATURE_CARD_MAX_LIMIT;
        };

        // Score-based multiplier - better score gets closer to max
        double scoreMultiplier;
        if (trustScore >= TRUST_SCORE_PREMIUM) {
            scoreMultiplier = 1.0;
        } else if (trustScore >= TRUST_SCORE_GOOD) {
            scoreMultiplier = 0.8;
        } else if (trustScore >= TRUST_SCORE_AVERAGE) {
            scoreMultiplier = 0.6;
        } else {
            scoreMultiplier = 0.4;
        }

        BigDecimal limitByScore = BigDecimal.valueOf(maxByType * scoreMultiplier);

        // Income-based limit : 2.5x monthly income
        BigDecimal limitByIncome = annualIncome.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(CREDIT_LIMIT_INCOME_MULTIPLIER));

        // take the lower of two
        return limitByScore.min(limitByIncome).setScale(0, RoundingMode.FLOOR);
    }

    private void validateCardTypeEligibility(CardType cardType, int trustScore) {
        boolean eligible = switch (cardType) {
            case CLASSIC -> trustScore >= 20;
            case GOLD -> trustScore >= TRUST_SCORE_AVERAGE;
            case PLATINUM -> trustScore >= TRUST_SCORE_GOOD;
            case SIGNATURE -> trustScore >= TRUST_SCORE_PREMIUM;
        };

        if (!eligible) {
            throw new InvalidOperationException(
                    "Your trust score of " + trustScore +
                            "is not sufficient for a " +
                            cardType + "credit card");
        }
    }

    private BigDecimal calculateRewardPoints(BigDecimal amount, SpendCategory category) {
        double rate = switch (category) {
            case DINING, TRAVEL -> REWARD_DINING;
            case SHOPPING -> REWARD_SHOPPING;
            case FUEL, GROCERY -> REWARD_FUEL;
            default -> REWARD_DEFAULT;
        };

        return amount.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(rate));
    }

    private BigDecimal calculateMinimumDue(BigDecimal outstanding) {
        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal percentage = outstanding.multiply(BigDecimal.valueOf(CREDIT_CARD_MIN_DUE_PERCENTAGE / 100));
        BigDecimal floor = BigDecimal.valueOf(CREDIT_CARD_MIN_DUE_FLOOR);

        return percentage.max(floor).setScale(2, RoundingMode.HALF_UP);
    }

    private String generateCardNumber() {
        // Generate 16 digit number using Luhn algorithm
        StringBuilder sb = new StringBuilder("4");
        Random random = new Random();

        for (int i = 0; i < 14; i++) {
            sb.append(random.nextInt(10));
        }

        // Calculate and append Luhn Check digit
        String partial = sb.toString();
        int checkDigit = calculateLuhnCheckDigit(partial);

        return partial + checkDigit;
    }

    private int calculateLuhnCheckDigit(String number) {
        int sum = 0;
        boolean alternate = false;  // starts false — first digit from right is NOT doubled
        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(number.charAt(i));
            if (alternate) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
            alternate = !alternate;
        }
        return (10 - (sum % 10)) % 10;
    }

    private String maskedCardNumber(String cardNumber) {
        return "**** **** ****" + cardNumber.substring(12);
    }

    private CreditCardResponse mapToCardResponse(CreditCard card) {
        return CreditCardResponse.builder()
                .id(card.getId())
                .maskedCardNumber(card.getMaskedCardNumber())
                .cardHolderName(card.getCardHolderName())
                .cardType(card.getCardType())
                .status(card.getStatus())
                .creditLimit(card.getCreditLimit())
                .availableLimit(card.getAvailableLimit())
                .outstandingAmount(card.getOutstandingAmount())
                .minimumDue(card.getMinimumDue())
                .totalRewardPoints(card.getTotalRewardPoints())
                .expiryDate(card.getExpiryDate())
                .nextBillingDate(card.getNextBillingDate())
                .paymentDueDate(card.getPaymentDueDate())
                .internationalTransactionsEnabled(card.isInternationalTransactionsEnabled())
                .onlineTransactionsEnabled(card.isOnlineTransactionsEnabled())
                .linkedAccountNumber(card.getLinkedAccount().getAccountNumber())
                .createdAt(card.getCreatedAt())
                .build();
    }

    private CardTransactionResponse mapToTransactionResponse(CardTransaction tx) {
        return CardTransactionResponse.builder()
                .id(tx.getId())
                .transactionReference(tx.getTransactionReference())
                .amount(tx.getAmount())
                .merchantName(tx.getMerchantName())
                .category(tx.getCategory())
                .rewardPointsEarned(tx.getRewardPointsEarned())
                .availableLimitAfter(tx.getAvailableLimitAfter())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build();
    }

    private BillingCycleResponse mapToBillingCycleResponse(BillingCycle cycle) {
        return BillingCycleResponse.builder()
                .id(cycle.getId())
                .cycleStartDate(cycle.getCycleStartDate())
                .cycleEndDate(cycle.getCycleEndDate())
                .paymentDueDate(cycle.getPaymentDueDate())
                .totalSpend(cycle.getTotalSpend())
                .minimumDue(cycle.getMinimumDue())
                .openingOutstanding(cycle.getOpeningOutstanding())
                .closingOutstanding(cycle.getClosingOutstanding())
                .totalPaid(cycle.getTotalPaid())
                .interestCharged(cycle.getInterestCharged())
                .latePaymentFee(cycle.getLatePaymentFee())
                .totalRewardPointsEarned(cycle.getTotalRewardPointsEarned())
                .status(cycle.getStatus())
                .daysOverdue(cycle.getDaysOverdue())
                .createdAt(cycle.getCreatedAt())
                .build();
    }
}