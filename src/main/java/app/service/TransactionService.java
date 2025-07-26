/* src/main/java/app/service/TransactionService.java */
package app.service;

import app.dao.TransactionDao;
import app.model.*;

import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public final class TransactionService {

    /* ─────────────────── stato ─────────────────── */
    private final TransactionDao dao;
    private final List<Transaction> cache;

    public TransactionService(TransactionDao dao) {
        this.dao = dao;
        try {  // prima lettura da disco
            this.cache = new ArrayList<>(dao.loadAll());
        } catch (Exception ex) {
            throw new RuntimeException("Errore caricamento dati", ex);
        }
    }

    /* ─────────────────── CRUD ─────────────────── */
    public void add(Transaction tx) {
        cache.add(tx);
        persist();
    }

    /** sostituisce una entry esistente con la versione editata */
    public void replace(Transaction original, Transaction edited) {
        int idx = cache.indexOf(original);
        if (idx >= 0) {
            cache.set(idx, edited);

            persist();
        }
    }

    /** rimpiazza l’intera collezione */
    public void replaceAll(Collection<Transaction> all) {
        cache.clear();
        cache.addAll(all);

        persist();
    }

    public List<Transaction> list() {
        return Collections.unmodifiableList(cache);
    }

    public MonthlyReport monthlyReport(YearMonth ym) {
        var filtered = cache.stream().filter(t -> YearMonth.from(t.date()).equals(ym)).toList();
        
        return buildReport(ym, filtered);
    }

    /* ─────────────────── report ─────────────────── */
    private static MonthlyReport buildReport(YearMonth ym, List<Transaction> txs) {
        Money in  = txs.stream()
            .filter(t -> t.type() == TxType.ENTRATA)
            .map(Transaction::amount)
            .reduce(Money.ZERO, Money::add);

        Money out = txs.stream()
            .filter(t -> t.type() == TxType.USCITA)
            .map(Transaction::amount)
            .reduce(Money.ZERO, Money::add);

        Map<String, Money> byCat = txs.stream().collect(Collectors.groupingBy(
            t -> t.category().name(),
            TreeMap::new,
            Collectors.reducing(Money.ZERO, Transaction::amount, Money::add)
        ));

        return new MonthlyReport(ym, in, out, in.subtract(out), byCat);
    }               

    /* ─────────────────── helper ─────────────────── */
    private void persist() {
        try { dao.saveAll(cache); }
        
        catch (Exception ex) {
            throw new RuntimeException("Errore salvataggio dati", ex);
        }
    }
}
