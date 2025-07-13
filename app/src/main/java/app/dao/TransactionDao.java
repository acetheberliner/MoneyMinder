package app.dao;

import app.model.Transaction;
import java.util.List;

public interface TransactionDao {
    List<Transaction> loadAll() throws DaoException;
    void saveAll(List<Transaction> txs) throws DaoException;
}
