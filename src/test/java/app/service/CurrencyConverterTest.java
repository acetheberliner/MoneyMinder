package app.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifica dei tassi di cambio fissi in CurrencyConverter.
 */
class CurrencyConverterTest {

    private static BigDecimal €(double v) { return BigDecimal.valueOf(v); }

    @Test
    void toEur_shouldReturnIdentityForEur() {
        assertEquals(€(50), CurrencyConverter.toEur(€(50), "EUR"));
    }

    @Test
    void usdAndGbpRatesAreApplied() {
        // 100 USD * 0.92 = 92 €
        assertEquals(0, CurrencyConverter.toEur(€(100), "USD").compareTo(€(92)));


        // 100 GBP * 1.17 = 117 €
        assertEquals(0, CurrencyConverter.toEur(€(100), "GBP").compareTo(€(117)));

    }

    @Test
    void unknownCurrencyFallsBackToIdentity() {
        assertEquals(€(42), CurrencyConverter.toEur(€(42), "ABC"));
    }
}
