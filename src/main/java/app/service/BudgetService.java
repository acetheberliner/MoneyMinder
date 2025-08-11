/* ───── app/service/BudgetService.java ───── */
package app.service;

import app.model.Money;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.*;

public final class BudgetService {

    private final File file;                        // <── percorso inject
    private final Map<String, Money> map = new TreeMap<>();
    private final ObjectMapper om  = new ObjectMapper();

    public BudgetService(File file) {
        this.file = Objects.requireNonNull(file);
        load();
    }

    // opzionale: costruttore di default (fallback)
    public BudgetService() {
        this(new File(System.getProperty("user.dir"), "data/money-minder-budgets.json"));
    }

    public Map<String, Money> all() { return Collections.unmodifiableMap(map); }
    public Optional<Money> get(String cat) { return Optional.ofNullable(map.get(cat)); }

    public void put(String cat, Money m) {
        if (m == null || m.value().signum() == 0) map.remove(cat);
        else map.put(cat, m);
        persist();
    }

    private void load() {
        if (!file.exists()) return;
        try {
            Map<String,String> raw = om.readValue(file, new TypeReference<>() {});
            raw.forEach((k,v) -> map.put(k, Money.of(v)));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void persist() {
        try {
            Map<String,String> raw = new TreeMap<>();
            map.forEach((k,v) -> raw.put(k, v.value().toPlainString()));
            file.getParentFile().mkdirs();
            om.writerWithDefaultPrettyPrinter().writeValue(file, raw);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
