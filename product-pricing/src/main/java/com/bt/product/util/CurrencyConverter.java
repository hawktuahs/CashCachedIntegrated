package com.bt.product.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class CurrencyConverter {

    private static final Map<String, BigDecimal> EXCHANGE_RATES = new HashMap<>();
    private static final String BASE_CURRENCY = "USD";

    static {
        EXCHANGE_RATES.put("USD", new BigDecimal("1.0"));
        EXCHANGE_RATES.put("EUR", new BigDecimal("0.92"));
        EXCHANGE_RATES.put("GBP", new BigDecimal("0.78"));
        EXCHANGE_RATES.put("INR", new BigDecimal("83.20"));
        EXCHANGE_RATES.put("KWD", new BigDecimal("0.31"));
        EXCHANGE_RATES.put("AED", new BigDecimal("3.67"));
        EXCHANGE_RATES.put("CAD", new BigDecimal("1.36"));
        EXCHANGE_RATES.put("JPY", new BigDecimal("149.50"));
        EXCHANGE_RATES.put("CNY", new BigDecimal("7.24"));
        EXCHANGE_RATES.put("MXN", new BigDecimal("18.40"));
        EXCHANGE_RATES.put("ZAR", new BigDecimal("18.20"));
        EXCHANGE_RATES.put("AUD", new BigDecimal("1.52"));
    }

    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        String normalizedFrom = normalizeCurrency(fromCurrency);
        String normalizedTo = normalizeCurrency(toCurrency);

        if (normalizedFrom.equals(normalizedTo)) {
            return amount;
        }

        BigDecimal fromRate = EXCHANGE_RATES.getOrDefault(normalizedFrom, BigDecimal.ONE);
        BigDecimal toRate = EXCHANGE_RATES.getOrDefault(normalizedTo, BigDecimal.ONE);

        BigDecimal usdAmount = amount.divide(fromRate, 4, RoundingMode.HALF_UP);
        BigDecimal convertedAmount = usdAmount.multiply(toRate);

        log.debug("Converting {} {} to {}: {} USD -> {} {}", 
                amount, normalizedFrom, normalizedTo, usdAmount, convertedAmount, normalizedTo);

        return convertedAmount.setScale(2, RoundingMode.HALF_UP);
    }

    public String getBaseCurrency() {
        return BASE_CURRENCY;
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return BASE_CURRENCY;
        }
        return currency.trim().toUpperCase();
    }
}
