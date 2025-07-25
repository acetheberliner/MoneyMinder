/* ───────── app/gui/TransactionDialog.java ───────── */
package app.gui;

import app.model.*;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Pattern;

public final class TransactionDialog {
    /* ───────── costanti ───────── */
    private static final Pattern AMOUNT_OK = Pattern.compile("\\d+(?:[,.]\\d{1,2})?");

    /* ───────── factory principale ───────── */
    public static Optional<Transaction> show(Transaction original) {
        Dialog<Transaction> dlg = new Dialog<>();

        dlg.setTitle(original == null ? "Nuova transazione" : "Modifica transazione");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        /* ---------- campi base ---------- */
        DatePicker dpDate = new DatePicker(original == null ? LocalDate.now() : original.date());

        ComboBox<TxType> cbType = new ComboBox<>();
        cbType.getItems().addAll(TxType.values());
        cbType.getSelectionModel().select(original == null ? TxType.ENTRATA : original.type());

        /* ---------- categorie ---------- */
        ComboBox<String> cbCat = new ComboBox<>();
        
        // enum + eventuali custom già inserite
        cbCat.setItems(FXCollections.observableArrayList(MainController.allCategoryNames()));

        if (original != null) cbCat.getSelectionModel().select(original.category().name());

        // tasto “+ Categoria” affiancato
        Button btnAddCat = new Button("+");
        btnAddCat.setOnAction(ev -> {
            TextInputDialog tid = new TextInputDialog();

            tid.setTitle("Nuova categoria");
            tid.setHeaderText(null);
            tid.setContentText("Nome categoria:");
            tid.showAndWait().ifPresent(name -> {
                name = name.strip();
                if (name.isBlank()) return;
                if (!MainController.CUSTOM_CATEGORIES.contains(name)) {
                    MainController.CUSTOM_CATEGORIES.add(name);
                    cbCat.getItems().add(name);
                }
                cbCat.getSelectionModel().select(name);
            });
        });

        /* ---------- importo & descrizione ---------- */
        String initAmt = original == null ? "" : original.amount().value().toPlainString().replace('.', ',');
        TextField tfAmount = new TextField(initAmt);
        tfAmount.setPromptText("es. 12,50");

        TextField tfDesc = new TextField(original == null ? "" : original.description());
        tfDesc.setPromptText("Descrizione");

        /* ---------- layout ---------- */
        GridPane gp = new GridPane();
        gp.setHgap(8); gp.setVgap(10); gp.setPadding(new Insets(15));

        gp.addRow(0, new Label("Data"), dpDate);
        gp.addRow(1, new Label("Tipo"), cbType);
        gp.addRow(2, new Label("Categoria"), new HBox(5, cbCat, btnAddCat));
        gp.addRow(3, new Label("Importo (€)"), tfAmount);
        gp.addRow(4, new Label("Descrizione"), tfDesc);

        dlg.getDialogPane().setContent(gp);

        /* ---------- validazione ---------- */
        Node okBtn = dlg.getDialogPane().lookupButton(ButtonType.OK);

        Runnable valid = () -> okBtn.setDisable(!AMOUNT_OK.matcher(tfAmount.getText().strip()).matches());
        valid.run();
        tfAmount.textProperty().addListener((o,oldVal,n)->valid.run());

        /* ---------- result ---------- */
        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;

            String raw = tfAmount.getText().strip().replace(',', '.');
            String catName = cbCat.getValue();
            Category cat;

            try { cat = Category.valueOf(catName); }
            catch (IllegalArgumentException e) { cat = Category.ALTRO; }

            return new Transaction(
                dpDate.getValue(),
                tfDesc.getText().strip(),
                cat,
                Money.of(raw),
                cbType.getValue()
            );
        });
        return dlg.showAndWait();
    }

    public static Optional<Transaction> show() { return show(null); }
    private TransactionDialog() {}
}
