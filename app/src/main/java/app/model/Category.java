// Category.java
package app.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public enum Category {
    @JsonAlias({"SALARY"})      Stipendio,
    @JsonAlias({"RENT"})        Affitto,
    @JsonAlias({"GROCERIES"})   Alimenti,
    @JsonAlias({"UTILITIES"})   Utilita,
    @JsonAlias({"ENTERTAIN"})   Intrattenimento,
    @JsonAlias({"FUEL"})        Carburante,
    @JsonAlias({"OTHER"})       Altro;
}
