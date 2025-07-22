package app.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import java.text.NumberFormat;
import java.util.Locale;

/** Value Object monetario in EUR. */
public record Money(BigDecimal value) {

    private static final NumberFormat IT_EUR = NumberFormat.getCurrencyInstance(Locale.ITALY);
    static { IT_EUR.setMinimumFractionDigits(2); }   //ex.: 1 500,00 €

    public Money {
        Objects.requireNonNull(value);
    }

    public Money add(Money o)      { return new Money(value.add(o.value)); }
    public Money subtract(Money o) { return new Money(value.subtract(o.value)); }
    
    @Override public String toString() { return IT_EUR.format(value); }

    public static Money of(String s) { return new Money(new BigDecimal(s)); }
    public static final Money ZERO = new Money(BigDecimal.ZERO);
}
