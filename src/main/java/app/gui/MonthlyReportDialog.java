package app.gui;

import app.model.MonthlyReport;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Map;

public final class MonthlyReportDialog {

    public static void show(MonthlyReport rep) {

        /* ---- totali ---- */
        Label lblIn  = new Label("Entrate:  " + rep.totaleEntrate());
        Label lblOut = new Label("Uscite:   " + rep.totaleEntrate());
        Label lblSal = new Label("Saldo:    " + rep.saldo());
        lblIn .setStyle("-fx-font-size:14; -fx-text-fill:#4caf50;");
        lblOut.setStyle("-fx-font-size:14; -fx-text-fill:#e53935;");
        lblSal.setStyle("-fx-font-size:16; -fx-font-weight:bold;");

        VBox totals = new VBox(6, lblIn, lblOut, lblSal);

        /* ---- tabella per categoria ---- */
        TableView<Map.Entry<String, ?>> tbl = new TableView<>();
        tbl.setPrefHeight(260);

        TableColumn<Map.Entry<String, ?>, String> colCat = new TableColumn<>("Categoria");
        colCat.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getKey()));

        TableColumn<Map.Entry<String, ?>, String> colVal = new TableColumn<>("Importo");
        colVal.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getValue().toString()));

        tbl.getColumns().addAll(colCat, colVal);
        tbl.getItems().setAll(rep.perCategoria().entrySet());

        /* ---- layout finestra ---- */
        VBox root = new VBox(12, totals, tbl);
        root.setPadding(new Insets(20));

        Stage st = new Stage();
        st.initModality(Modality.APPLICATION_MODAL);
        st.setTitle("Report " + rep.month());
        st.setScene(new Scene(root, 300, 380));
        st.showAndWait();
    }

    private MonthlyReportDialog() {}
}
