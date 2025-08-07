package app.service;

import app.model.Money;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test automatici per BudgetService (persistenza e logica soglie).
 */
class BudgetServiceTest {

    private String oldHome;        // per ripristinare user.home
    private BudgetService srv;

    @BeforeEach
    void setUp(@TempDir Path tmp) {
        /* Isoliamo il file .money-minder-budgets.json in una cartella usa-e-getta */
        oldHome = System.getProperty("user.home");
        System.setProperty("user.home", tmp.toString());

        srv = new BudgetService();
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.home", oldHome);
    }

    /* ------------- TEST ------------- */

    @Test
    void putAndGet_shouldPersistBetweenInstances() {
        srv.put("ALIMENTI", Money.of("100"));

        // nuova istanza → deve rileggere il file appena scritto
        BudgetService reloaded = new BudgetService();
        assertEquals("100", reloaded.get("ALIMENTI").orElseThrow().value().toPlainString());
    }

    @Test
    void put_zeroOrNullShouldRemoveEntry() {
        srv.put("UTILITA", Money.of(BigDecimal.TEN));
        srv.put("UTILITA", Money.ZERO);      // azzera

        assertTrue(srv.all().isEmpty());
    }

    @Test
    void all_shouldBeUnmodifiable() {
        assertThrows(UnsupportedOperationException.class,
                     () -> srv.all().put("X", Money.ZERO));
    }
}
