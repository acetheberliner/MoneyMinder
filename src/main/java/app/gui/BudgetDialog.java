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
                    String txt = tf.getText().strip();
                    Money m = txt.isBlank() ? Money.ZERO : Money.of(txt.replace(',', '.'));
                    srv.put(cat, m);
                }
            });
        }

        Stage st = new Stage();

        st.initOwner(owner);
        st.setTitle("Budget mensile per categoria");
        st.setScene(new Scene(new ScrollPane(gp), 320, 320));

        st.show();
    }
    private BudgetDialog() {}
}
