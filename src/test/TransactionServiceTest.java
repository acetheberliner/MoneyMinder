package test;

import it.unibo.moneyminder.dao.TransactionDao;
import it.unibo.moneyminder.model.*;
import it.unibo.moneyminder.service.TransactionService;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

class TransactionServiceTest {

    static class InMemDao implements TransactionDao {
        List<Transaction> data = new ArrayList<>();
        public List<Transaction> loadAll() { return new ArrayList<>(data); }
        public void saveAll(List<Transaction> txs) { data = new ArrayList<>(txs); }
    }

    @Test void balanceCorrect() {
        var svc = new TransactionService(new InMemDao());
        svc.add(new Transaction(LocalDate.of(2025,7,1),"", Category.SALARY, Money.of("1000"), TxType.INCOME));
        svc.add(new Transaction(LocalDate.of(2025,7,2),"", Category.GROCERIES, Money.of("200"), TxType.EXPENSE));
        var r = svc.monthlyReport(YearMonth.of(2025,7));
        Assertions.assertEquals("800.00 €", r.balance().toString());
    }
}
