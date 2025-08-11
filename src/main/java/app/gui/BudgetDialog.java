package app.gui;

import app.model.Money;
import app.service.BudgetService;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.util.List;

public final class BudgetDialog {
    public static void show(Stage owner, List<String> categories, BudgetService srv) {
        GridPane gp = new GridPane();

        gp.setHgap(10);
        gp.setVgap(8);
        gp.setPadding(new Insets(15));

        int row = 0;

        for (String cat : categories) {
            TextField tf = new TextField();
            srv.get(cat).ifPresent(m -> tf.setText(m.toString()));
            gp.addRow(row++, new Label(cat), tf);

            /* salva parametri on-focus-lost */
            tf.focusedProperty().addListener((o, oldF, newF) -> {
                if (!newF) {
                    String raw = tf.getText();

                    // normalizza: togli simboli/whitespace e uniforma i separatori
                    String txt = raw == null ? "" : raw
                        .strip()
                        .replace("€", "")
                        .replace("\u00A0", "")   // NBSP
                        .replace(" ", "");

                    // Se c'è sia '.' che ',', assumo formato it (1.234,56): tolgo i punti e metto '.' come decimale
                    int lastComma = txt.lastIndexOf(',');
                    int lastDot   = txt.lastIndexOf('.');
                    if (lastComma > lastDot) {
                        txt = txt.replace(".", "").replace(',', '.');
                    } else {
                        // Altrimenti tolgo eventuali ',' come separatori migliaia
                        txt = txt.replace(",", "");
                    }

                    Money m = txt.isBlank() ? Money.ZERO : Money.of(txt);
                    srv.put(cat, m);
                }
            });
        }

        Stage st = new Stage();

        st.initOwner(owner);
        st.setTitle("Budget mensile per categoria");
        st.setScene(new Scene(new ScrollPane(gp), 350, 320));

        st.show();
    }
    private BudgetDialog() {}
}
