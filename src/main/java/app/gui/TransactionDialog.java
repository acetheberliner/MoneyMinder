// package app.gui;

// import app.model.*;
// import javafx.geometry.Insets;
// import javafx.scene.Node;
// import javafx.scene.control.*;
// import javafx.scene.layout.GridPane;

// import java.time.LocalDate;
// import java.util.Optional;

// public final class TransactionDialog {

//     public static Optional<Transaction> show() {
//         /* ---- dialog base ---- */
//         Dialog<Transaction> dialog = new Dialog<>();
//         dialog.setTitle("Nuova transazione");
//         dialog.getDialogPane().getButtonTypes()
//               .addAll(ButtonType.OK, ButtonType.CANCEL);

//         /* ---- campi ---- */
//         DatePicker dpDate = new DatePicker(LocalDate.now());

//         ComboBox<TxType> cbType = new ComboBox<>();
//         cbType.getItems().addAll(TxType.values());
//         cbType.getSelectionModel().select(TxType.USCITA);

//         ComboBox<Category> cbCat = new ComboBox<>();
//         cbCat.getItems().addAll(Category.values());
//         cbCat.getSelectionModel().select(Category.ALTRO);

//         TextField tfAmount = new TextField();
//         tfAmount.setPromptText("es. 12,50");

//         TextField tfDesc = new TextField();
//         tfDesc.setPromptText("Descrizione");

//         /* ---- layout ---- */
//         GridPane gp = new GridPane();
//         gp.setHgap(10);
//         gp.setVgap(10);
//         gp.setPadding(new Insets(15));

//         gp.addRow(0, new Label("Data"),        dpDate);
//         gp.addRow(1, new Label("Tipo"),        cbType);
//         gp.addRow(2, new Label("Categoria"),   cbCat);
//         gp.addRow(3, new Label("Importo (€)"), tfAmount);
//         gp.addRow(4, new Label("Descrizione"), tfDesc);

//         dialog.getDialogPane().setContent(gp);

//         /* ---- validazione importo ---- */
//         Node okBtn = dialog.getDialogPane().lookupButton(ButtonType.OK);
//         okBtn.setDisable(true);
//         tfAmount.textProperty().addListener((obs, o, n) ->
//             okBtn.setDisable(!n.matches("\\d+(?:[\\.,]\\d{1,2})?"))
//         );

//         /* ---- conversione ---- */
//         dialog.setResultConverter(bt -> {
//             if (bt != ButtonType.OK) return null;
//             String amtRaw = tfAmount.getText().replace(',', '.');
//             return new Transaction(
//                     dpDate.getValue(),
//                     tfDesc.getText(),
//                     cbCat.getValue(),
//                     Money.of(amtRaw),
//                     cbType.getValue()
//             );
//         });

//         return dialog.showAndWait();
//     }
// }

/* ───────── app/gui/TransactionDialog.java ───────── */
/* ───────── app/gui/TransactionDialog.java ───────── */
package app.gui;

import app.model.*;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Pattern;

public final class TransactionDialog {

    /* accetta “123”, “123,45”, “123.45” */
    private static final Pattern AMOUNT_OK = Pattern.compile("\\d+(?:[,.]\\d{1,2})?");

    /** Nuova o modifica: se {@code original==null} crea, altrimenti edita */
    public static Optional<Transaction> show(Transaction original) {

        Dialog<Transaction> dlg = new Dialog<>();
        dlg.setTitle(original == null ? "Nuova transazione" : "Modifica transazione");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        /* ---------- campi ---------- */
        DatePicker dpDate = new DatePicker(original == null ? LocalDate.now() : original.date());

        ComboBox<TxType> cbType = new ComboBox<>();
        cbType.getItems().addAll(TxType.values());
        cbType.getSelectionModel().select(original == null ? TxType.ENTRATA : original.type());

        ComboBox<Category> cbCat = new ComboBox<>();
        cbCat.getItems().addAll(Category.values());
        cbCat.getSelectionModel().select(original == null ? Category.STIPENDIO : original.category());

        /* importo “pulito” senza simbolo € */
        String initAmt = original == null
                ? ""
                : original.amount().value().toPlainString().replace('.', ',');
        TextField tfAmount = new TextField(initAmt);
        tfAmount.setPromptText("es. 12,50");

        TextField tfDesc = new TextField(original == null ? "" : original.description());
        tfDesc.setPromptText("Descrizione");

        /* ---------- layout ---------- */
        GridPane gp = new GridPane();
        gp.setHgap(8);
        gp.setVgap(10);
        gp.setPadding(new Insets(15));
        gp.addRow(0, new Label("Data"),        dpDate);
        gp.addRow(1, new Label("Tipo"),        cbType);
        gp.addRow(2, new Label("Categoria"),   cbCat);
        gp.addRow(3, new Label("Importo (€)"), tfAmount);
        gp.addRow(4, new Label("Descrizione"), tfDesc);
        dlg.getDialogPane().setContent(gp);

        /* ---------- validazione ---------- */
        Node okBtn = dlg.getDialogPane().lookupButton(ButtonType.OK);

        Runnable validate = () ->
                okBtn.setDisable(!AMOUNT_OK.matcher(tfAmount.getText().strip()).matches());

        validate.run();                               // stato iniziale
        tfAmount.textProperty().addListener((o,oldVal,newVal) -> validate.run());

        /* ---------- conversione ---------- */
        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;

            String clean = tfAmount.getText().strip().replace(',', '.');
            return new Transaction(
                    dpDate.getValue(),
                    tfDesc.getText().strip(),
                    cbCat.getValue(),
                    Money.of(clean),
                    cbType.getValue()
            );
        });

        return dlg.showAndWait();
    }

    /** overload comodo per “Aggiungi” */
    public static Optional<Transaction> show() { return show(null); }
}
