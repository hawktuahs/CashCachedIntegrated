package com.bt.accounts.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bt.accounts.config.CashCachedProperties;
import com.bt.accounts.dto.CashCachedBalanceResponse;
import com.bt.accounts.dto.CashCachedIssueRequest;
import com.bt.accounts.dto.CashCachedRedeemRequest;
import com.bt.accounts.dto.CashCachedSummaryResponse;
import com.bt.accounts.dto.CashCachedTransferRequest;
import com.bt.accounts.entity.CashCachedLedgerEntry;
import com.bt.accounts.entity.CashCachedLedgerEntry.Operation;
import com.bt.accounts.entity.CashCachedWallet;
import com.bt.accounts.repository.CashCachedLedgerRepository;
import com.bt.accounts.repository.CashCachedWalletRepository;
import com.bt.accounts.exception.InvalidAccountDataException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashCachedService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final Map<String, BigDecimal> STATIC_RATE_TABLE = Map.ofEntries(
            Map.entry("USD", new BigDecimal("1.00")),
            Map.entry("KWD", new BigDecimal("0.31")),
            Map.entry("INR", new BigDecimal("83.20")),
            Map.entry("GBP", new BigDecimal("0.78")),
            Map.entry("EUR", new BigDecimal("0.92")),
            Map.entry("AED", new BigDecimal("3.67")),
            Map.entry("CAD", new BigDecimal("1.36")),
            Map.entry("JPY", new BigDecimal("149.50")),
            Map.entry("CNY", new BigDecimal("7.24")),
            Map.entry("MXN", new BigDecimal("18.40")),
            Map.entry("ZAR", new BigDecimal("18.20")));

    private final CashCachedProperties properties;
    private final CashCachedLedgerRepository ledgerRepository;
    private final CashCachedWalletRepository walletRepository;
    private final CustomerProfileClient customerProfileClient;

    @Transactional
    public CashCachedLedgerEntry issue(CashCachedIssueRequest request) {
        BigDecimal amount = requireWholeAmount(request.getAmount());
        CashCachedWallet wallet = ensureWallet(request.getCustomerId());
        String transactionId = generateTransactionId();
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);
        return ledgerRepository.save(CashCachedLedgerEntry.builder()
                .customerId(request.getCustomerId())
                .changeAmount(amount)
                .balanceAfter(wallet.getBalance())
                .operation(Operation.ISSUE)
                .transactionHash(transactionId)
                .reference(request.getReference())
                .build());
    }

    @Transactional
    public CashCachedLedgerEntry recordContractLock(String customerId, BigDecimal amount, String reference) {
        BigDecimal debitAmount = requireWholeAmount(amount);
        CashCachedWallet wallet = ensureWallet(customerId);
        
        if (wallet.getBalance().compareTo(debitAmount) < 0) {
            throw new IllegalStateException("Insufficient wallet balance");
        }
        
        wallet.setBalance(wallet.getBalance().subtract(debitAmount));
        walletRepository.save(wallet);
        
        return ledgerRepository.save(CashCachedLedgerEntry.builder()
                .customerId(customerId)
                .changeAmount(debitAmount.negate())
                .balanceAfter(wallet.getBalance())
                .operation(Operation.CONTRACT)
                .reference(reference)
                .build());
    }

    @Transactional
    public void addInterestToTreasury(BigDecimal amount, String reference) {
        BigDecimal roundedAmount = requireWholeAmount(amount);
        recordTreasuryIssuance(roundedAmount, reference);
    }

    @Transactional
    public TransferResult transfer(CashCachedTransferRequest request) {
        BigDecimal amount = requireWholeAmount(request.getAmount());
        if (request.getFromCustomerId().equals(request.getToCustomerId())) {
            throw new IllegalArgumentException("Transfers require distinct customers");
        }
        CashCachedWallet fromWallet = ensureWallet(request.getFromCustomerId());
        CashCachedWallet toWallet = ensureWallet(request.getToCustomerId());
        if (fromWallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance for transfer");
        }
        fromWallet.setBalance(fromWallet.getBalance().subtract(amount));
        toWallet.setBalance(toWallet.getBalance().add(amount));
        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);
        CashCachedLedgerEntry debit = ledgerRepository.save(CashCachedLedgerEntry.builder()
                .customerId(request.getFromCustomerId())
                .changeAmount(amount.negate())
                .balanceAfter(fromWallet.getBalance())
                .operation(Operation.TRANSFER_OUT)
                .reference(request.getReference())
                .build());

        CashCachedLedgerEntry credit = ledgerRepository.save(CashCachedLedgerEntry.builder()
                .customerId(request.getToCustomerId())
                .changeAmount(amount)
                .balanceAfter(toWallet.getBalance())
                .operation(Operation.TRANSFER_IN)
                .reference(request.getReference())
                .build());

        return new TransferResult(debit, credit);
    }

    @Transactional
    public CashCachedLedgerEntry redeem(CashCachedRedeemRequest request) {
        BigDecimal amount = requireWholeAmount(request.getAmount());
        CashCachedWallet wallet = ensureWallet(request.getCustomerId());
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InvalidAccountDataException("Insufficient wallet balance");
        }
        String transactionId = generateTransactionId();
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
        return ledgerRepository.save(CashCachedLedgerEntry.builder()
                .customerId(request.getCustomerId())
                .changeAmount(amount.negate())
                .balanceAfter(wallet.getBalance())
                .operation(Operation.REDEEM)
                .transactionHash(transactionId)
                .reference(request.getReference())
                .build());
    }

    @Transactional(readOnly = true)
    public List<CashCachedLedgerEntry> history(String customerId) {
        return ledgerRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Transactional(readOnly = true)
    public Page<CashCachedLedgerEntry> historyAll(Pageable pageable) {
        return ledgerRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public CashCachedBalanceResponse balance(String customerId, String authToken) {
        CashCachedWallet wallet = walletRepository.findByCustomerId(customerId)
                .orElse(null);
        
        BigDecimal balance = wallet != null ? wallet.getBalance() : ZERO;
        String userBaseCurrency = wallet != null && wallet.getBaseCurrency() != null 
                ? wallet.getBaseCurrency() 
                : "INR";

        CashCachedBalanceResponse response = new CashCachedBalanceResponse();
        response.setCustomerId(customerId);
        response.setBalance(balance);
        response.setBaseCurrency(userBaseCurrency);
        response.setBaseValue(balance);

        Map<String, BigDecimal> rates = resolveRates();
        response.setRates(rates);

        String displayCurrency = resolvePreferredCurrency(authToken, customerId);
        response.setTargetCurrency(displayCurrency);

        String normalizedBase = normalizeCurrency(userBaseCurrency);
        String normalizedDisplay = normalizeCurrency(displayCurrency);
        BigDecimal baseRate = rates.getOrDefault(normalizedBase, BigDecimal.ONE);
        BigDecimal displayRate = rates.getOrDefault(normalizedDisplay, BigDecimal.ONE);
        
        log.info("Currency conversion for customer {}: {} {} -> {} (base rate: {}, display rate: {})", 
                customerId, balance, userBaseCurrency, displayCurrency, baseRate, displayRate);
        
        BigDecimal usdValue = balance.divide(baseRate, 4, RoundingMode.HALF_UP);
        BigDecimal targetValue = usdValue.multiply(displayRate);
        
        log.info("Converted: {} USD -> {} {}", usdValue, targetValue, displayCurrency);
        
        response.setTargetValue(targetValue);

        return response;
    }

    @Transactional(readOnly = true)
    public BigDecimal totalSupply() {
        return walletRepository.findAll().stream()
                .map(CashCachedWallet::getBalance)
                .reduce(ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public BigDecimal ledgerTotal() {
        return ledgerRepository.findAll().stream()
                .map(CashCachedLedgerEntry::getChangeAmount)
                .reduce(ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public String resolveWalletBaseCurrency(String customerId) {
        if (!StringUtils.hasText(customerId)) {
            return normalizeCurrency(properties.getBaseCurrency());
        }

        return walletRepository.findByCustomerId(customerId)
                .map(CashCachedWallet::getBaseCurrency)
                .filter(StringUtils::hasText)
                .map(this::normalizeCurrency)
                .orElseGet(() -> normalizeCurrency(customerProfileClient
                        .fetchPreferredCurrency(customerId, null)
                        .orElse(properties.getBaseCurrency())));
    }

    @Transactional
    public CashCachedLedgerEntry creditWallet(String customerId, BigDecimal amount, String reference) {
        return creditWallet(customerId, amount, null, reference);
    }

    @Transactional
    public CashCachedLedgerEntry creditWallet(String customerId, BigDecimal amount, String currency, String reference) {
        CashCachedWallet wallet = ensureWallet(customerId);
        String userBaseCurrency = wallet.getBaseCurrency() != null ? wallet.getBaseCurrency() : "INR";
        
        BigDecimal amountInBaseCurrency = convertCurrency(amount, currency, userBaseCurrency);
        BigDecimal roundedAmount = amountInBaseCurrency.setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalAmount = requireWholeAmount(roundedAmount);
        wallet.setBalance(wallet.getBalance().add(finalAmount));
        walletRepository.save(wallet);
        
        String ref = reference;
        if (currency != null && !currency.equalsIgnoreCase(userBaseCurrency)) {
            ref = String.format("%s (%.2f %s)", reference, amount, currency);
        }
        
        return ledgerRepository.save(CashCachedLedgerEntry.builder()
                .customerId(customerId)
                .changeAmount(finalAmount)
                .balanceAfter(wallet.getBalance())
                .operation(Operation.TRANSFER_IN)
                .reference(ref)
                .build());
    }

    @Transactional
    public CashCachedLedgerEntry debitWallet(String customerId, BigDecimal amount, String reference) {
        return debitWallet(customerId, amount, null, reference);
    }

    @Transactional
    public CashCachedLedgerEntry debitWallet(String customerId, BigDecimal amount, String currency, String reference) {
        CashCachedWallet wallet = ensureWallet(customerId);
        String userBaseCurrency = wallet.getBaseCurrency() != null ? wallet.getBaseCurrency() : "INR";
        
        BigDecimal amountInBaseCurrency = convertCurrency(amount, currency, userBaseCurrency);
        BigDecimal roundedAmount = amountInBaseCurrency.setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalAmount = requireWholeAmount(roundedAmount);
        if (wallet.getBalance().compareTo(finalAmount) < 0) {
            throw new InvalidAccountDataException("Insufficient wallet balance");
        }
        wallet.setBalance(wallet.getBalance().subtract(finalAmount));
        walletRepository.save(wallet);
        
        String ref = reference;
        if (currency != null && !currency.equalsIgnoreCase(userBaseCurrency)) {
            ref = String.format("%s (%.2f %s)", reference, amount, currency);
        }
        
        return ledgerRepository.save(CashCachedLedgerEntry.builder()
                .customerId(customerId)
                .changeAmount(finalAmount.negate())
                .balanceAfter(wallet.getBalance())
                .operation(Operation.TRANSFER_OUT)
                .reference(ref)
                .build());
    }

    @Transactional(readOnly = true)
    public CashCachedSummaryResponse summary() {
        BigDecimal ledgerTotal = ledgerTotal();
        BigDecimal totalSupply = totalSupply();
        CashCachedSummaryResponse response = new CashCachedSummaryResponse();
        response.setContractAddress(properties.getContractAddress());
        response.setTreasuryAddress(properties.getTreasuryAddress());
        response.setLedgerTotal(ledgerTotal);
        response.setOnChainSupply(totalSupply);
        response.setVariance(ledgerTotal.subtract(totalSupply));
        return response;
    }

    private CashCachedWallet ensureWallet(String customerId) {
        return walletRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    String userBaseCurrency = customerProfileClient
                            .fetchPreferredCurrency(customerId, null)
                            .orElse("INR");
                    return walletRepository.save(CashCachedWallet.builder()
                            .customerId(customerId)
                            .balance(ZERO)
                            .baseCurrency(userBaseCurrency)
                            .build());
                });
    }

    private String generateTransactionId() {
        return "TXN-" + java.util.UUID.randomUUID().toString();
    }

    private void recordTreasuryIssuance(BigDecimal amount, String reference) {
        String transactionId = generateTransactionId();
        String treasuryId = properties.getTreasuryAddress();
        CashCachedWallet treasuryWallet = ensureWallet(treasuryId);
        treasuryWallet.setBalance(treasuryWallet.getBalance().add(amount));
        walletRepository.save(treasuryWallet);
        ledgerRepository.save(CashCachedLedgerEntry.builder()
                .customerId(treasuryId)
                .changeAmount(amount)
                .balanceAfter(treasuryWallet.getBalance())
                .operation(Operation.ISSUE)
                .transactionHash(transactionId)
                .reference(reference)
                .build());
    }

    private BigDecimal requireWholeAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        return normalized;
    }

    @Getter
    @RequiredArgsConstructor
    public static class TransferResult {
        private final CashCachedLedgerEntry debitEntry;
        private final CashCachedLedgerEntry creditEntry;
    }


    private Map<String, BigDecimal> resolveRates() {
        Map<String, BigDecimal> rates = new HashMap<>();
        STATIC_RATE_TABLE.forEach((code, rate) -> rates.put(normalizeCurrency(code), rate.setScale(4, RoundingMode.HALF_UP)));

        String base = normalizeCurrency(properties.getBaseCurrency());
        rates.putIfAbsent(base, BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP));

        properties.getSupportedCurrencies().stream()
                .map(this::normalizeCurrency)
                .forEach(code -> rates.putIfAbsent(code, STATIC_RATE_TABLE.getOrDefault(code,
                        BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP))));

        return Collections.unmodifiableMap(rates);
    }

    private String normalizeCurrency(String code) {
        if (code == null || code.isBlank()) {
            return properties.getBaseCurrency();
        }
        return code.trim().toUpperCase();
    }

    private String resolvePreferredCurrency(String authToken, String customerId) {
        String baseCurrency = normalizeCurrency(properties.getBaseCurrency());
        if (!StringUtils.hasText(customerId)) {
            return baseCurrency;
        }

        return customerProfileClient.fetchPreferredCurrency(customerId, authToken)
                .map(this::normalizeCurrency)
                .filter(StringUtils::hasText)
                .orElse(baseCurrency);
    }

    public BigDecimal convertCurrency(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null || amount.compareTo(ZERO) <= 0) {
            return ZERO;
        }
        
        String normalizedFrom = normalizeCurrency(fromCurrency);
        String normalizedTo = normalizeCurrency(toCurrency);
        
        if (normalizedFrom.equals(normalizedTo)) {
            return amount;
        }
        
        Map<String, BigDecimal> rates = resolveRates();
        BigDecimal fromRate = rates.getOrDefault(normalizedFrom, BigDecimal.ONE);
        BigDecimal toRate = rates.getOrDefault(normalizedTo, BigDecimal.ONE);
        
        BigDecimal usdAmount = amount.divide(fromRate, 4, RoundingMode.HALF_UP);
        BigDecimal convertedAmount = usdAmount.multiply(toRate);
        
        log.info("Converting {} {} to {}: {} USD -> {} {}", 
                amount, normalizedFrom, normalizedTo, usdAmount, convertedAmount, normalizedTo);
        
        return convertedAmount;
    }

}
