/* ───────── app/gui/MonthlyReportDialog.java ───────── */
package app.gui;

import app.model.Money;
import app.model.MonthlyReport;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.time.YearMonth;
import java.util.Map;

public final class MonthlyReportDialog {
    public static void show(Stage owner, YearMonth ym, MonthlyReport rep) {
        /* ---- riepilogo numerico ---- */
        GridPane gp = new GridPane();

        gp.setHgap(8);
        gp.setVgap(8);
        gp.setPadding(new Insets(15));

        gp.addColumn(0,
                new Label("Entrate :  " + rep.totaleEntrate()),
                new Label("Uscite  :  " + rep.totaleUscite()),
                new Label("Saldo   :  " + rep.saldo())
        );

        /* ---- tabella per categoria ---- */
        TableView<Map.Entry<String, Money>> tbl = new TableView<>();
        TableColumn<Map.Entry<String, Money>, String> colCat = new TableColumn<>("Categoria");
        TableColumn<Map.Entry<String, Money>, String> colTot = new TableColumn<>("Totale");

        colCat.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getKey()));
        colTot.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getValue().toString()));

        tbl.getColumns().clear();

        java.util.Collections.addAll(tbl.getColumns(), colCat, colTot);
        tbl.getItems().setAll(rep.perCategoria().entrySet());
        tbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        gp.add(tbl, 1, 0, 1, 3);

        /* ---- stage ---- */
        Stage st = new Stage();

        st.initOwner(owner);
        st.setTitle("Report " + ym);
        st.setScene(new Scene(gp, 450, 320));
        st.show();
    }
    private MonthlyReportDialog() {}
}
