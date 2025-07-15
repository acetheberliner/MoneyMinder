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
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.File;
import java.time.YearMonth;

public final class MainController {

    /* ---------- FXML ---------- */
    @FXML private TableView<Transaction> table;
    @FXML private TableColumn<Transaction, String> colDate, colType, colCat, colAmount, colDesc;
    @FXML private PieChart pieExpense;
    @FXML private PieChart pieIncome;

    /* ---------- Service ---------- */
    private final TransactionService service = new TransactionService(
        new JsonTransactionDao(new File(System.getProperty("user.home"), ".money-minder.json")));

    /* ---------- Init ---------- */
    @FXML
    private void initialize() {
        colDate  .setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().date().toString()));
        colType  .setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().type().toString()));
        colCat   .setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().category().name()));
        colAmount.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().amount().toString()));
        colDesc  .setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().description()));

        table.setItems(FXCollections.observableArrayList(service.list()));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Nessuna transazione registrata"));

        refreshCharts(YearMonth.now());
    }

    /* ---------- Toolbar actions ---------- */
    @FXML private void onAdd() {
        TransactionDialog.show().ifPresent(tx -> {
            service.add(tx);
            table.getItems().add(tx);
            refreshCharts(YearMonth.from(tx.date()));
        });
    }

    @FXML private void onRemove() {
        Transaction sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        var conf = new Alert(Alert.AlertType.CONFIRMATION,
            "Rimuovere la transazione selezionata?");
        if (conf.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            table.getItems().remove(sel);
            service.replaceAll(table.getItems());
            refreshCharts(YearMonth.from(sel.date()));
        }
    }

    /* ---------- Helper piecharts ---------- */
    private void refreshCharts(YearMonth ym) {
        var rep = service.monthlyReport(ym);

        /* --- USCITE per categoria --- */
        ObservableList<PieChart.Data> outData = FXCollections.observableArrayList();
        rep.byCategory().forEach((cat, val) ->
            outData.add(new PieChart.Data(cat + " " + val, val.value().doubleValue())));
        pieExpense.setData(outData);
        pieExpense.setLegendVisible(false);

        /* --- ENTRATE totali --- */
        ObservableList<PieChart.Data> inData = FXCollections.observableArrayList(
            new PieChart.Data("Entrate " + rep.income(), rep.income().value().doubleValue()));
        pieIncome.setData(inData);
        pieIncome.setLegendVisible(false);
    }
}
