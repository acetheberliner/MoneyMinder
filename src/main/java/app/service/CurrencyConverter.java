package app.service;

import java.math.BigDecimal;
import java.util.Map;

public final class CurrencyConverter {

    /** tasso (1 unità della valuta → €) */
    private static final Map<String, BigDecimal> RATE = Map.of(
        "EUR", BigDecimal.valueOf(1),
        "USD", BigDecimal.valueOf(0.92),
        "GBP", BigDecimal.valueOf(1.17)
    );

    /** converte  {@code amount}  da {@code cur}  in EUR  */
    public static BigDecimal toEur(BigDecimal amount, String cur) {
        return amount.multiply(RATE.getOrDefault(cur, BigDecimal.ONE));
    }

    private CurrencyConverter() {}
}
