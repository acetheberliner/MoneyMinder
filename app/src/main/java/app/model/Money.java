package app.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Value Object monetario (EUR di default). */
public record Money(BigDecimal value) {

    public Money {
        Objects.requireNonNull(value);
    }

    public Money add(Money other) {
        return new Money(value.add(other.value));
    }
    public Money subtract(Money other) {
        return new Money(value.subtract(other.value));
    }
    @Override public String toString() {
        return value.setScale(2, RoundingMode.HALF_EVEN) + " €";
    }

    public static Money of(String s) {     // helper per parse rapida
        return new Money(new BigDecimal(s));
    }
    public static final Money ZERO = new Money(BigDecimal.ZERO);
}
