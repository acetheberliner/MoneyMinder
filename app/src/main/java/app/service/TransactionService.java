package app.service;

import app.dao.TransactionDao;
import app.model.*;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public final class TransactionService {

    private final TransactionDao dao;
    private final List<Transaction> cache;

    public TransactionService(TransactionDao dao) {
        this.dao = dao;
        try { this.cache = new ArrayList<>(dao.loadAll()); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    /* ---------- API pubblica ---------- */

    public void add(Transaction tx) {
        cache.add(tx);
        persist();
    }

    public List<Transaction> list() {
        return Collections.unmodifiableList(cache);
    }

    public MonthlyReport monthlyReport(YearMonth ym) {
        var filtered = cache.stream()
                .filter(t -> YearMonth.from(t.date()).equals(ym))
                .toList();
        return buildReport(filtered);               // <<< qui il nuovo nome
    }

    /* ---------- DTO report ---------- */

    public record MonthlyReport(
            Money income,
            Money expense,
            Map<String, Money> byCategory
    ) {
        public Money balance() { return income.subtract(expense); }
    }

    /* ---------- helper interni ---------- */

    private void persist() {
        try { dao.saveAll(cache); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    private static MonthlyReport buildReport(List<Transaction> txs) {   // <<< rename OK
        Money income = txs.stream()
        .filter(t -> t.type() == TxType.ENTRATA)   // prima INCOME
        .map(Transaction::amount)
        .reduce(Money.ZERO, Money::add);

        Money expense = txs.stream()
        .filter(t -> t.type() == TxType.USCITA)    // prima EXPENSE
        .map(Transaction::amount)
        .reduce(Money.ZERO, Money::add);

        Map<String, Money> byCat = txs.stream().collect(Collectors.groupingBy(
                t -> t.category().name(),
                TreeMap::new,
                Collectors.reducing(Money.ZERO, Transaction::amount, Money::add)
        ));
        return new MonthlyReport(income, expense, byCat);
    }

    public void replaceAll(Collection<Transaction> txs) {
        cache.clear();
        cache.addAll(txs);
        persist();
    }
}
