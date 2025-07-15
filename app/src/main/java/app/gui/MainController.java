package app.gui;

import app.dao.JsonTransactionDao;
import app.model.*;
import app.service.TransactionService;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;

import java.io.File;
import java.time.YearMonth;

public final class MainController {

    /* ---------- FXML injection ---------- */
    @FXML private TableView<Transaction> table;
    @FXML private TableColumn<Transaction, String> colDate, colType, colCat, colAmount, colDesc;
    @FXML private PieChart pie;

    /* ---------- Service & state ---------- */
    private final TransactionService service =
        new TransactionService(new JsonTransactionDao(
            new File(System.getProperty("user.home"), ".money-minder.json")));

    /* ---------- Init ---------- */
    @FXML
    private void initialize() {
        colDate  .setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().date().toString()));
        colType  .setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().type().name()));
        colCat   .setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().category().name()));
        colAmount.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().amount().toString()));
        colDesc  .setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().description()));

        table.setItems(FXCollections.observableArrayList(service.list()));
        refreshPie(YearMonth.now());
    }

    /* ---------- Toolbar actions ---------- */
    @FXML private void onAdd() {
        // TODO: sostituire con vero dialogo
        service.add(new Transaction(java.time.LocalDate.now(), "Caffè",
                Category.OTHER, Money.of("1.50"), TxType.EXPENSE));
        table.getItems().setAll(service.list());
        refreshPie(YearMonth.now());
    }

    @FXML private void onRemove() {
        Transaction sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        table.getItems().remove(sel);
        // TODO: aggiornare anche il service + persist
    }

    @FXML private void onReport() {
        refreshPie(YearMonth.now());
    }

    /* ---------- helper ---------- */
    private void refreshPie(YearMonth ym) {
        var rep = service.monthlyReport(ym);
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        rep.byCategory().forEach((k, v) ->
            data.add(new PieChart.Data(k + " " + v, v.value().doubleValue())));
        pie.setData(data);
    }
}
