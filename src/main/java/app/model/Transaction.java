package app.model;

import java.time.LocalDate;

public record Transaction(
        LocalDate date,
        String description,
        Category category,
        Money amount,
        TxType type
) {}
