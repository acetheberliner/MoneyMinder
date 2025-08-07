package app.service;

import app.dao.TransactionDao;
import app.model.*;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test automatici per TransactionService.
 */
class TransactionServiceTest {

    /** DAO fittizio: in-memory, zero I/O. */
    private static class StubDao implements TransactionDao {
        final List<Transaction> store = new ArrayList<>();

        @Override public List<Transaction> loadAll() { return store; }
        @Override public void saveAll(List<Transaction> xs) { store.clear(); store.addAll(xs); }}

    private StubDao dao;          // inizializzato fresh ad ogni test
    private TransactionService svc;

    /* ---------------- helpers ---------------- */
    private static Money €(double v) { return Money.of(BigDecimal.valueOf(v)); }

    private static Transaction tx(LocalDate d, double amt, String cat, TxType t) {
        return new Transaction(d, "desc", cat, €(amt), t);
    }

    @BeforeEach
    void setUp() {
        dao = new StubDao();
        svc = new TransactionService(dao);
    }

    /* ------------------- TESTS ------------------- */

    @Test
    void add_shouldPersistAndReturnInList() {
        Transaction t = tx(LocalDate.now(), 100.00, "STIPENDIO", TxType.ENTRATA);

        svc.add(t);

        assertEquals(1, svc.list().size());

        Transaction stored = svc.list().get(0);
        assertEquals(t.date(), stored.date());
        assertEquals(t.type(), stored.type());
        assertEquals(0, stored.amount().value().compareTo(t.amount().value()));
    }

    @Test
    void replace_shouldSubstituteElement() {
        Transaction original = tx(LocalDate.now(), 50, "AFFITTO", TxType.USCITA);
        svc.add(original);

        Transaction edited   = tx(LocalDate.now(), 45, "AFFITTO", TxType.USCITA);
        svc.replace(original, edited);

        assertFalse(svc.list().contains(original));
        assertTrue (svc.list().contains(edited));
    }

    @Test
    void monthlyReport_shouldAggregateCorrectly() {
        LocalDate d1 = LocalDate.of(2025,  8,  1);   // agosto
        LocalDate d2 = LocalDate.of(2025,  8, 15);
        LocalDate d3 = LocalDate.of(2025,  9,  1);   // settembre

        svc.add(tx(d1, 1000, "STIPENDIO", TxType.ENTRATA));
        svc.add(tx(d2,   40, "ALIMENTI",  TxType.USCITA));
        svc.add(tx(d3,  500, "STIPENDIO", TxType.ENTRATA)); // altro mese

        MonthlyReport rep = svc.monthlyReport(YearMonth.of(2025, 8));

        assertEquals(€(1000).value(), rep.totaleEntrate().value());
        assertEquals(€(  40).value(), rep.totaleUscite().value());
        assertEquals(€( 960).value(), rep.saldo().value());
        assertTrue (rep.perCategoria().containsKey("ALIMENTI"));
    }

    @Test
    void list_shouldBeUnmodifiable() {
        svc.add(tx(LocalDate.now(), 10, "ALTRO", TxType.USCITA));

        List<Transaction> lst = svc.list();
        assertThrows(UnsupportedOperationException.class,
                     () -> lst.add(tx(LocalDate.now(), 1, "ALTRO", TxType.USCITA)));
    }
}
