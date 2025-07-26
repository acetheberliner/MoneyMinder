/* ───── app/service/BudgetService.java ───── */
package app.service;

import app.model.Money;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.*;

public final class BudgetService {

    /* file JSON di persistenza nel profilo utente */
    private static final File FILE = new File(System.getProperty("user.home"), ".money-minder-budgets.json");

    private final Map<String, Money> map = new TreeMap<>();
    private final ObjectMapper om  = new ObjectMapper();

    public BudgetService() { load(); }

    public Map<String, Money> all() { return Collections.unmodifiableMap(map); }
    public Optional<Money> get(String cat) { return Optional.ofNullable(map.get(cat)); }
    
    public void put(String cat, Money m) {
        if (m == null || m.value().signum() == 0)
            map.remove(cat);
        else
            map.put(cat, m);
        persist();
    }

    private void load() {
        if (!FILE.exists()) return;
        try {
            Map<String,String> raw = om.readValue(FILE, new TypeReference<>() {});
            raw.forEach((k,v) -> map.put(k, Money.of(v)));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void persist() {
        try {
            Map<String,String> raw = new TreeMap<>();
            map.forEach((k,v) -> raw.put(k, v.value().toPlainString()));
            om.writerWithDefaultPrettyPrinter().writeValue(FILE, raw);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
