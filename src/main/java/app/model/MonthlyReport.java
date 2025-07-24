package app.model;

import java.time.YearMonth;
import java.util.Map;

public record MonthlyReport(
        Money totalIncome,
        Money totalExpense,
        Map<String, Money> byCategory
) {
    public Money balance()          { return totalIncome.subtract(totalExpense); }

    /* alias per CLI legacy --- evita errori di “method not found” */
    public Money income()           { return totalIncome; }
    public Money expense()          { return totalExpense; }

    public String totaleEntrate() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'totaleEntrate'");
    }

    public String saldo() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'saldo'");
    }

    public String month() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'month'");
    }

    public Map perCategoria() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'perCategoria'");
    }
}