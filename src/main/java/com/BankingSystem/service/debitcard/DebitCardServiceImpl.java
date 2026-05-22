package com.BankingSystem.service.debitcard;

import com.BankingSystem.BankConfig;
import com.BankingSystem.dto.request.debitcard.DebitCardPinChangeRequest;
import com.BankingSystem.dto.request.debitcard.DebitCardRequest;
import com.BankingSystem.dto.request.debitcard.DebitCardTransactionRequest;
import com.BankingSystem.dto.response.debitcard.DebitCardResponse;
import com.BankingSystem.dto.response.debitcard.DebitCardTransactionResponse;
import com.BankingSystem.entity.account.Account;
import com.BankingSystem.entity.card.*;
import com.BankingSystem.entity.transactions.Transaction;
import com.BankingSystem.entity.users.User;
import com.BankingSystem.entity.transactions.TransactionType;
import com.BankingSystem.entity.transactions.TransactionStatus;
import com.BankingSystem.exception.InvalidOperationException;
import com.BankingSystem.exception.ResourceNotFoundException;
import com.BankingSystem.repo.*;
import com.BankingSystem.util.notifications.NotificationEvent;
import com.BankingSystem.util.notifications.NotificationEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DebitCardServiceImpl implements DebitCardService {

    private final DebitCardRepository debitCardRepository;
    private final DebitCardTransactionRepository debitCardTransactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher eventPublisher;

    private static final String ATM_SPENT_KEY = "DC_ATM_SPENT:";
    private static final String POS_SPENT_KEY = "DC_POS_SPENT:";
    private static final String ONLINE_SPENT_KEY = "DC_ONLINE_SPENT:";
    private static final int MAX_CARDS_PER_ACCOUNT = BankConfig.MAX_CARDS_PER_ACCOUNT;

    @Override
    @Transactional
    public DebitCardResponse issueCard(DebitCardRequest request) {

        Account account = accountRepository.findByAccountNumberAndIsActiveTrue(request.getAccountNumber()).orElseThrow(() -> new ResourceNotFoundException("Account not found: " + request.getAccountNumber()));

        // Check if account already has this specific card type
        if (debitCardRepository.existsByAccountAndCardType(
                account, request.getCardType())) {
            throw new InvalidOperationException("This account already has a " + request.getCardType() + " debit card.");
        }

        // Check maximum cards per account
        long existingCardCount = debitCardRepository.countByAccount(account);
        if (existingCardCount >= MAX_CARDS_PER_ACCOUNT) {
            throw new InvalidOperationException("Maximum of " + MAX_CARDS_PER_ACCOUNT + " debit cards allowed per account.");
        }

        User user = account.getUser();

        BigDecimal atmLimit = BigDecimal.valueOf(
                request.getCardType() == DebitCardType.CLASSIC
                        ? BankConfig.CLASSIC_DEBIT_ATM_DAILY_LIMIT
                        : BankConfig.PLATINUM_DEBIT_ATM_DAILY_LIMIT);

        BigDecimal posLimit = BigDecimal.valueOf(
                request.getCardType() == DebitCardType.CLASSIC
                        ? BankConfig.CLASSIC_DEBIT_POS_DAILY_LIMIT
                        : BankConfig.PLATINUM_DEBIT_POS_DAILY_LIMIT);

        BigDecimal onlineLimit = BigDecimal.valueOf(
                request.getCardType() == DebitCardType.CLASSIC
                        ? BankConfig.CLASSIC_DEBIT_ONLINE_DAILY_LIMIT
                        : BankConfig.PLATINUM_DEBIT_ONLINE_DAILY_LIMIT);

        String cardNumber = generateCardNumber();

        DebitCard card = DebitCard.builder()
                .cardNumber(cardNumber)
                .maskedCardNumber(maskCardNumber(cardNumber))
                .cardHolderName(user.getFirstName() + " " +
                        user.getLastName())
                .cardPin(passwordEncoder.encode(request.getPin()))
                .user(user)
                .account(account)
                .cardType(request.getCardType())
                .status(DebitCardStatus.ACTIVE)
                .dailyAtmLimit(atmLimit)
                .dailyPosLimit(posLimit)
                .dailyOnlineLimit(onlineLimit)
                .internationalTransactionsEnabled(false)
                .onlineTransactionsEnabled(true)
                .contactlessEnabled(true)
                .expiryDate(LocalDate.now().plusYears(BankConfig.DEBIT_CARD_EXPIRY_YEARS))
                .build();

        DebitCard saved = debitCardRepository.save(card);

        Map<String, Object> data = new HashMap<>();
        data.put("cardType", saved.getCardType().toString());
        data.put("maskedCardNumber", saved.getMaskedCardNumber());

        eventPublisher.publishEvent(NotificationEvent.forUser(this,
                NotificationEventType.CARD_ACTIVATED,
                user.getFirstName() + " " + user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                data));

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public DebitCardTransactionResponse processTransaction(DebitCardTransactionRequest request) {

        DebitCard card = debitCardRepository.findByCardNumberWithLock(request.getCardNumber()).orElseThrow(() -> new ResourceNotFoundException("Card not found: " + request.getCardNumber()));

        if (card.getStatus() != DebitCardStatus.ACTIVE) {
            return saveFailedTransaction(card, request, DebitCardTransactionStatus.FAILED_CARD_BLOCKED);
        }

        if (request.getTransactionType() == DebitTransactionType.ONLINE_PURCHASE && !card.isOnlineTransactionsEnabled()) {
            throw new InvalidOperationException("Online transactions are disabled on this card.");
        }

        if (!passwordEncoder.matches(request.getPin(), card.getCardPin())) {
            return saveFailedTransaction(card, request, DebitCardTransactionStatus.FAILED_INVALID_PIN);
        }

        if (!checkAndUpdateDailyLimit(card, request.getTransactionType(), request.getAmount())) {
            return saveFailedTransaction(card, request, DebitCardTransactionStatus.FAILED_DAILY_LIMIT_EXCEEDED);
        }

        Account account = accountRepository.findByAccountNumberWithLock(card.getAccount().getAccountNumber()).orElseThrow(() -> new ResourceNotFoundException("Linked account not found"));

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            return saveFailedTransaction(card, request, DebitCardTransactionStatus.FAILED_INSUFFICIENT_BALANCE);
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);

        DebitCardTransaction dcTx = DebitCardTransaction.builder()
                .transactionReference("DC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .debitCard(card)
                .amount(request.getAmount())
                .balanceAfterTransaction(account.getBalance())
                .merchantName(request.getMerchantName())
                .transactionType(request.getTransactionType())
                .status(DebitCardTransactionStatus.SUCCESS)
                .description(request.getDescription() != null ? request.getDescription() : "Purchase at " + request.getMerchantName())
                .build();

        DebitCardTransaction saved = debitCardTransactionRepository.save(dcTx);

        transactionRepository.save(Transaction.builder()
                .transactionReference(saved.getTransactionReference())
                .transactionType(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.SUCCESS)
                .amount(request.getAmount())
                .balanceAfterTransaction(account.getBalance())
                .account(account)
                .description("Debit card: " + request.getMerchantName())
                .build());

        Map<String, Object> data = new HashMap<>();
        data.put("amount", request.getAmount());
        data.put("merchantName", request.getMerchantName());
        data.put("accountNumber", account.getAccountNumber());
        data.put("balance", account.getBalance());
        data.put("transactionReference", saved.getTransactionReference());

        eventPublisher.publishEvent(NotificationEvent.forUser(this,
                NotificationEventType.WITHDRAWAL,
                card.getCardHolderName(),
                card.getUser().getEmail(),
                card.getUser().getPhoneNumber(),
                data));

        return mapToTransactionResponse(saved);
    }

    @Override
    @Transactional
    public DebitCardResponse blockCard(String cardNumber) {
        DebitCard card = debitCardRepository.findByCardNumber(cardNumber).orElseThrow(() -> new ResourceNotFoundException("Card not found: " + cardNumber));

        if (card.getStatus() == DebitCardStatus.CANCELLED) {
            throw new InvalidOperationException("Cancelled cards cannot be blocked.");
        }

        card.setStatus(DebitCardStatus.BLOCKED);
        DebitCard saved = debitCardRepository.save(card);

        Map<String, Object> data = new HashMap<>();
        data.put("maskedCardNumber", saved.getMaskedCardNumber());
        data.put("cardType", "DEBIT");

        eventPublisher.publishEvent(NotificationEvent.forUser(this,
                NotificationEventType.CARD_BLOCKED,
                saved.getCardHolderName(),
                saved.getUser().getEmail(),
                saved.getUser().getPhoneNumber(),
                data));

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public DebitCardResponse unblockCard(String cardNumber) {
        DebitCard card = debitCardRepository.findByCardNumber(cardNumber).orElseThrow(() -> new ResourceNotFoundException("Card not found: " + cardNumber));

        if (card.getStatus() != DebitCardStatus.BLOCKED) {
            throw new InvalidOperationException("Card is not blocked.");
        }

        card.setStatus(DebitCardStatus.ACTIVE);
        return mapToResponse(debitCardRepository.save(card));
    }


    @Override
    @Transactional
    public DebitCardResponse changePin(DebitCardPinChangeRequest request) {
        DebitCard card = debitCardRepository.findByCardNumber(request.getCardNumber()).orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        if (!passwordEncoder.matches(request.getCurrentPin(), card.getCardPin())) {
            throw new InvalidOperationException("Current PIN is incorrect.");
        }

        if (request.getCurrentPin().equals(request.getNewPin())) {
            throw new InvalidOperationException("New PIN cannot be the same as current PIN.");
        }

        card.setCardPin(passwordEncoder.encode(request.getNewPin()));
        return mapToResponse(debitCardRepository.save(card));
    }

    @Override
    @Transactional
    public DebitCardResponse toggleInternationalTransactions(String cardNumber, boolean enabled) {
        DebitCard card = debitCardRepository.findByCardNumber(cardNumber).orElseThrow(() -> new ResourceNotFoundException("Card not found: " + cardNumber));

        card.setInternationalTransactionsEnabled(enabled);
        return mapToResponse(debitCardRepository.save(card));
    }

    @Override
    @Transactional
    public DebitCardResponse toggleOnlineTransactions(String cardNumber, boolean enabled) {
        DebitCard card = debitCardRepository.findByCardNumber(cardNumber).orElseThrow(() -> new ResourceNotFoundException("Card not found: " + cardNumber));

        card.setOnlineTransactionsEnabled(enabled);
        return mapToResponse(debitCardRepository.save(card));
    }

    @Override
    public DebitCardResponse getCardDetails(String cardNumber) {
        return mapToResponse(debitCardRepository.findByCardNumber(cardNumber).orElseThrow(() -> new ResourceNotFoundException("Card not found: " + cardNumber)));
    }

    @Override
    public List<DebitCardResponse> getUserCards(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        return debitCardRepository.findByUser(user)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DebitCardTransactionResponse> getTransactionHistory(String cardNumber) {
        DebitCard card = debitCardRepository.findByCardNumber(cardNumber).orElseThrow(() -> new ResourceNotFoundException("Card not found: " + cardNumber));

        return debitCardTransactionRepository
                .findByDebitCardOrderByCreatedAtDesc(card)
                .stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    // ── Daily limit management ────────────────────────────────────────────

    private boolean checkAndUpdateDailyLimit(DebitCard card, DebitTransactionType type, BigDecimal amount) {
        String key = getLimitPrefix(type) + card.getCardNumber();
        BigDecimal dailyLimit = getDailyLimit(card, type);

        String spentStr = redisTemplate.opsForValue().get(key);
        BigDecimal alreadySpent = spentStr != null ? new BigDecimal(spentStr) : BigDecimal.ZERO;

        if (alreadySpent.add(amount).compareTo(dailyLimit) > 0) {
            return false;
        }

        long secondsUntilMidnight = LocalTime.now().until(LocalTime.MIDNIGHT, ChronoUnit.SECONDS);
        if (secondsUntilMidnight <= 0) secondsUntilMidnight = 86400;

        redisTemplate.opsForValue().set(key, alreadySpent.add(amount).toString(), Duration.ofSeconds(secondsUntilMidnight));

        return true;
    }

    private BigDecimal getDailySpent(DebitCard card, DebitTransactionType type) {
        String key = getLimitPrefix(type) + card.getCardNumber();
        String spentStr = redisTemplate.opsForValue().get(key);
        return spentStr != null ? new BigDecimal(spentStr) : BigDecimal.ZERO;
    }

    private String getLimitPrefix(DebitTransactionType type) {
        return switch (type) {
            case ATM_WITHDRAWAL -> ATM_SPENT_KEY;
            case POS_PURCHASE, CONTACTLESS_PURCHASE -> POS_SPENT_KEY;
            case ONLINE_PURCHASE -> ONLINE_SPENT_KEY;
        };
    }

    private BigDecimal getDailyLimit(DebitCard card, DebitTransactionType type) {

        return switch (type) {
            case ATM_WITHDRAWAL -> card.getDailyAtmLimit();
            case POS_PURCHASE, CONTACTLESS_PURCHASE -> card.getDailyPosLimit();
            case ONLINE_PURCHASE -> card.getDailyOnlineLimit();
        };
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private DebitCardTransactionResponse saveFailedTransaction(DebitCard card, DebitCardTransactionRequest request, DebitCardTransactionStatus failStatus) {

        DebitCardTransaction failedTx = DebitCardTransaction.builder()
                .transactionReference("DC-FAIL-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .debitCard(card)
                .amount(request.getAmount())
                .balanceAfterTransaction(card.getAccount().getBalance())
                .merchantName(request.getMerchantName())
                .transactionType(request.getTransactionType())
                .status(failStatus)
                .description("FAILED: " + failStatus.name())
                .build();

        debitCardTransactionRepository.save(failedTx);
        return mapToTransactionResponse(failedTx);
    }

    private String generateCardNumber() {
        StringBuilder sb = new StringBuilder("4");
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 14; i++) {
            sb.append(random.nextInt(10));
        }
        String partial = sb.toString();
        int checkDigit = calculateLuhnCheckDigit(partial);
        return partial + checkDigit;
    }

    private int calculateLuhnCheckDigit(String number) {
        int sum = 0;
        boolean alternate = false;
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

    private String maskCardNumber(String cardNumber) {
        return "**** **** **** " + cardNumber.substring(12);
    }

    private DebitCardResponse mapToResponse(DebitCard card) {
        return DebitCardResponse.builder()
                .id(card.getId())
                .maskedCardNumber(card.getMaskedCardNumber())
                .cardHolderName(card.getCardHolderName())
                .cardType(card.getCardType())
                .status(card.getStatus())
                .dailyAtmLimit(card.getDailyAtmLimit())
                .dailyPosLimit(card.getDailyPosLimit())
                .dailyOnlineLimit(card.getDailyOnlineLimit())
                .dailyAtmSpent(getDailySpent(card,
                        DebitTransactionType.ATM_WITHDRAWAL))
                .dailyPosSpent(getDailySpent(card,
                        DebitTransactionType.POS_PURCHASE))
                .dailyOnlineSpent(getDailySpent(card,
                        DebitTransactionType.ONLINE_PURCHASE))
                .internationalTransactionsEnabled(
                        card.isInternationalTransactionsEnabled())
                .onlineTransactionsEnabled(
                        card.isOnlineTransactionsEnabled())
                .contactlessEnabled(card.isContactlessEnabled())
                .linkedAccountNumber(
                        card.getAccount().getAccountNumber())
                .accountBalance(card.getAccount().getBalance())
                .expiryDate(card.getExpiryDate())
                .createdAt(card.getCreatedAt())
                .build();
    }

    private DebitCardTransactionResponse mapToTransactionResponse(DebitCardTransaction tx) {

        return DebitCardTransactionResponse.builder()
                .id(tx.getId())
                .transactionReference(tx.getTransactionReference())
                .amount(tx.getAmount())
                .balanceAfterTransaction(tx.getBalanceAfterTransaction())
                .merchantName(tx.getMerchantName())
                .transactionType(tx.getTransactionType())
                .status(tx.getStatus())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}