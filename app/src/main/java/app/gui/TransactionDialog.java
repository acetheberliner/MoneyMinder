package app.gui;

import app.model.*;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public final class TransactionDialog {

    public static Optional<Transaction> show() {
        // --- dialog base ------------------------------------
        Dialog<Transaction> dialog = new Dialog<>();
        dialog.setTitle("Nuova transazione");
        dialog.getDialogPane().getButtonTypes()
              .addAll(ButtonType.OK, ButtonType.CANCEL);

        // --- campi ------------------------------------------
        DatePicker dpDate      = new DatePicker(LocalDate.now());
        ComboBox<TxType> cbType= new ComboBox<>();
        cbType.getItems().addAll(TxType.values());
        cbType.getSelectionModel().select(TxType.EXPENSE);

        ComboBox<Category> cbCat = new ComboBox<>();
        cbCat.getItems().addAll(Category.values());
        cbCat.getSelectionModel().select(Category.OTHER);

        TextField tfAmount = new TextField();
        tfAmount.setPromptText("es. 12.50");

        TextField tfDesc   = new TextField();
        tfDesc.setPromptText("Descrizione");

        // --- layout -----------------------------------------
        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(10); gp.setPadding(new Insets(15));
        gp.addRow(0, new Label("Data"), dpDate);
        gp.addRow(1, new Label("Tipo"), cbType);
        gp.addRow(2, new Label("Categoria"), cbCat);
        gp.addRow(3, new Label("Importo (€)"), tfAmount);
        gp.addRow(4, new Label("Descrizione"), tfDesc);
        dialog.getDialogPane().setContent(gp);

        // --- abilitazione pulsante OK solo se amount valido --
        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);
        tfAmount.textProperty().addListener((obs, o, n) ->
            okButton.setDisable(!n.matches("\\d+(\\.\\d{1,2})?"))
        );

        // --- convert result ---------------------------------
        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            return new Transaction(
                    dpDate.getValue(),
                    tfDesc.getText(),
                    cbCat.getValue(),
                    Money.of(tfAmount.getText()),
                    cbType.getValue()
            );
        });

        return dialog.showAndWait();
    }
}
