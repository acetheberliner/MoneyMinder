package app.model;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record Transaction(
        LocalDate date,
        String    description,

        @JsonAlias("category")          // <── ACCETTA anche “category”
        String    categoryName,

        Money     amount,
        TxType    type
) {
    /* enum di servizio – usato in grafici, colori, ecc. */
    public Category categoryEnum() {
        try { return Category.valueOf(categoryName); }
        catch (Exception ex) { return Category.ALTRO; }
    }

    /* ----- costruttore per Jackson (record + alias) ----- */
    @JsonCreator
    public Transaction(
            @JsonProperty("date")         LocalDate date,
            @JsonProperty("description")  String    description,
            @JsonProperty("categoryName") @JsonAlias("category") String categoryName,
            @JsonProperty("amount")       Money     amount,
            @JsonProperty("type")         TxType    type) {
        this.date         = date;
        this.description  = description;
        this.categoryName = categoryName;
        this.amount       = amount;
        this.type         = type;
    }
}
