package app.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public enum TxType {
    @JsonAlias("INCOME")   ENTRATA,
    @JsonAlias("EXPENSE")  USCITA ;

    @Override public String toString() {
        return switch (this) {
            case ENTRATA -> "Entrata";
            case USCITA  -> "Uscita";
        };
    }
}
