package app.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public enum Category {
    @JsonAlias("SALARY")        STIPENDIO,
    @JsonAlias("RENT")          AFFITTO,
    @JsonAlias("GROCERIES")     ALIMENTI,
    @JsonAlias("UTILITIES")     UTILITA,
    @JsonAlias("ENTERTAIN")     INTRATTENIMENTO,
    @JsonAlias("FUEL")          CARBURANTE,
    @JsonAlias("OTHER")         ALTRO;
}
