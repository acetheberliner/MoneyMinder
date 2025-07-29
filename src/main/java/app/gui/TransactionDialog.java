/* ───────── app/gui/TransactionDialog.java ───────── */
package app.gui;

import app.model.*;
import app.service.CurrencyConverter;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Pattern;

public final class TransactionDialog {

    /* accetta 123  – 123,45 – 123.45 */
    private static final Pattern AMOUNT_OK = Pattern.compile("\\d+(?:[,.]\\d{1,2})?");

    /** se {@code original==null} ⇒ “Nuova”, altrimenti “Modifica” */
    public static Optional<Transaction> show(Transaction original) {

        Dialog<Transaction> dlg = new Dialog<>();
        dlg.setTitle(original == null ? "Nuova transazione" : "Modifica transazione");
        dlg.getDialogPane()
           .getButtonTypes()
           .addAll(ButtonType.OK, ButtonType.CANCEL);

        /* ---------- campi ---------- */
        DatePicker dpDate = new DatePicker(
                original == null ? LocalDate.now() : original.date());

        ComboBox<TxType> cbType = new ComboBox<>();
        cbType.getItems().addAll(TxType.values());
        cbType.getSelectionModel().select(
                original == null ? null : original.type());

        /* ----- categoria (enum + custom) ----- */
        ComboBox<String> cbCat = new ComboBox<>();
        cbCat.setItems(FXCollections.observableArrayList(
                MainController.allCategoryNames()));
        if (original != null)
            cbCat.getSelectionModel().select(original.categoryName());

        Button btnAddCat = new Button("+");
        btnAddCat.setOnAction(e -> {
            TextInputDialog td = new TextInputDialog();
            td.setTitle("Nuova categoria");
            td.setHeaderText(null);
            td.setContentText("Nome categoria:");
            td.showAndWait().ifPresent(name -> {
                name = name.strip();
                if (name.isBlank()) return;

                if (!MainController.CUSTOM_CATEGORIES.contains(name)) {
                    MainController.CUSTOM_CATEGORIES.add(name);
                    cbCat.getItems().add(name);
                }
                cbCat.getSelectionModel().select(name);
            });
        });

        /* ----- importo & descrizione ----- */
        String initAmt = original == null ? ""
                       : original.amount().value().toPlainString()
                                         .replace('.', ',');
        TextField tfAmount = new TextField(initAmt);
        tfAmount.setPromptText("es. 12,50");

        TextField tfDesc = new TextField(
                original == null ? "" : original.description());
        tfDesc.setPromptText("Descrizione");

        /* ----- valuta ----- */
        ComboBox<String> cbCur = new ComboBox<>();
        cbCur.getItems().addAll("EUR", "USD", "GBP", "CHF");
        cbCur.getSelectionModel().select("EUR");

        /* ---------- layout ---------- */
        GridPane gp = new GridPane();
        gp.setHgap(8);
        gp.setVgap(10);
        gp.setPadding(new Insets(15));

        gp.addRow(0, new Label("Data"),        dpDate);
        gp.addRow(1, new Label("Tipologia"),        cbType);
        gp.addRow(2, new Label("Categoria"),   new HBox(5, cbCat, btnAddCat));
        gp.addRow(3, new Label("Importo"),     tfAmount);
        gp.addRow(4, new Label("Valuta"),      cbCur);
        gp.addRow(5, new Label("Descrizione"), tfDesc);

        /* anteprima in Euro */
        Label lblPreview = new Label();
        gp.add(lblPreview, 1, 6);

        Runnable refreshPreview = () -> {
            String txt = tfAmount.getText().strip().replace(',', '.');
            if (txt.matches("\\d+(?:[.]\\d{1,2})?")) {
                BigDecimal val  = new BigDecimal(txt);
                BigDecimal eur = CurrencyConverter.toEur(val, cbCur.getValue());
                lblPreview.setText("≈ " + Money.of(eur));
            } else lblPreview.setText("");
        };
        tfAmount.textProperty().addListener((o,ov,nv)->refreshPreview.run());
        cbCur.valueProperty().addListener((o,ov,nv)->refreshPreview.run());
        refreshPreview.run();

        dlg.getDialogPane().setContent(gp);

        /* ---------- validazione ---------- */
        Node ok = dlg.getDialogPane().lookupButton(ButtonType.OK);
        Runnable validate = () ->
                ok.setDisable(!AMOUNT_OK.matcher(tfAmount.getText().strip()).matches()
                               || cbType.getValue() == null
                               || cbCat.getValue() == null);
        validate.run();
        tfAmount.textProperty().addListener((o,ov,nv)->validate.run());
        cbType.valueProperty().addListener((o,ov,nv)->validate.run());
        cbCat.valueProperty().addListener((o,ov,nv)->validate.run());

        /* ---------- result ---------- */
        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;

            String raw = tfAmount.getText().strip().replace(',', '.');
            BigDecimal eur = CurrencyConverter.toEur(
                    new BigDecimal(raw), cbCur.getValue());

            return new Transaction(
                    dpDate.getValue(),
                    tfDesc.getText().strip(),
                    cbCat.getValue(),          // salva ESATTAMENTE il testo scelto
                    Money.of(eur),
                    cbType.getValue()
            );
        });

        return dlg.showAndWait();
    }

    /** overload comodo per “Aggiungi” */
    public static Optional<Transaction> show() { return show(null); }

    private TransactionDialog() {}
}
