/* ───── app/service/BudgetService.java ───── */
package app.service;

import app.model.Money;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.*;

public final class BudgetService {

    /* file JSON di persistenza nel profilo utente */
    private static final File FILE =
            new File(System.getProperty("user.home"), ".money-minder-budgets.json");

    private final Map<String, Money> map = new TreeMap<>();
    private final ObjectMapper       om  = new ObjectMapper();

    public BudgetService() { load(); }

    /* ───────── API ───────── */

    public Map<String, Money> all()            { return Collections.unmodifiableMap(map); }

    public Optional<Money> get(String cat)     { return Optional.ofNullable(map.get(cat)); }

    /** Crea / aggiorna un budget mensile per categoria.  
     *  Se l’importo è {@code null} o 0 ⇒ nessun tetto (viene rimosso). */
    public void put(String cat, Money m) {
        if (m == null || m.value().signum() == 0)   // Money.ZERO
            map.remove(cat);
        else
            map.put(cat, m);
        persist();
    }

    /* ───────── I/O JSON ───────── */

    private void load() {
        if (!FILE.exists()) return;
        try {
            /* JSON come <stringa, stringa> → converto in Money */
            Map<String,String> raw = om.readValue(FILE, new TypeReference<>() {});
            raw.forEach((k,v) -> map.put(k, Money.of(v)));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void persist() {
        try {
            /* serializzo Money come stringa “plain” */
            Map<String,String> raw = new TreeMap<>();
            map.forEach((k,v) -> raw.put(k, v.value().toPlainString()));
            om.writerWithDefaultPrettyPrinter().writeValue(FILE, raw);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
