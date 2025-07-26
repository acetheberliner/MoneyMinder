package app.model;

import java.time.YearMonth;
import java.util.Map;

public record MonthlyReport(
        YearMonth mese,
        Money      totaleEntrate,
        Money      totaleUscite,
        Money      saldo,
        Map<String, Money> perCategoria
) {

}
