package app.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import app.model.Transaction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class JsonTransactionDao implements TransactionDao {

    private final File file;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public JsonTransactionDao(File file) { this.file = file; }

    @Override
    public List<Transaction> loadAll() throws DaoException {
        if (!file.exists()) return new ArrayList<>();
        try {
            return mapper.readValue(file, new TypeReference<>() {});
        } catch (IOException e) {
            throw new DaoException("Errore lettura JSON", e);
        }
    }

    @Override
    public void saveAll(List<Transaction> txs) throws DaoException {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, txs);
        } catch (IOException e) {
            throw new DaoException("Errore scrittura JSON", e);
        }
    }
}
