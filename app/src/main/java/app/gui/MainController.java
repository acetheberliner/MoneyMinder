package app.gui;

import app.dao.JsonTransactionDao;
import app.model.*;
import app.service.TransactionService;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Map;
import java.util.stream.Collectors;

public final class MainController {

    /* FXML */
    @FXML private TableView<Transaction> table;
    @FXML private TableColumn<Transaction,String> colDate,colType,colCat,colAmount,colDesc;
    @FXML private DatePicker monthPicker;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<Category> cbFilterCat;
    @FXML private ComboBox<TxType>   cbFilterType;
    @FXML private PieChart pieIncome,pieExpense;
    @FXML private Label lblSaldo;

    /* dati */
    private final TransactionService service = new TransactionService(
            new JsonTransactionDao(new File(System.getProperty("user.home"), ".money-minder.json")));
    private final ObservableList<Transaction> master = FXCollections.observableArrayList();
    private final FilteredList<Transaction> view = new FilteredList<>(master, t -> true);

    /* init */
    @FXML
    private void initialize() {
        colDate  .setCellValueFactory(c->new ReadOnlyStringWrapper(c.getValue().date().toString()));
        colType  .setCellValueFactory(c->new ReadOnlyStringWrapper(c.getValue().type().toString()));
        colCat   .setCellValueFactory(c->new ReadOnlyStringWrapper(c.getValue().category().name()));
        colAmount.setCellValueFactory(c->new ReadOnlyStringWrapper(c.getValue().amount().toString()));
        colDesc  .setCellValueFactory(c->new ReadOnlyStringWrapper(c.getValue().description()));

        master.setAll(service.list());
        table.setItems(view);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        cbFilterCat.getItems().add(null);
        cbFilterCat.getItems().addAll(Category.values());
        cbFilterType.getItems().add(null);
        cbFilterType.getItems().addAll(TxType.values());

        monthPicker.setValue(LocalDate.now().withDayOfMonth(1));

        master.addListener((ListChangeListener<Transaction>) c -> refreshCharts(currentMonth()));
        applyFilters();
    }

    /* toolbar */
    @FXML private void onAdd() {
        TransactionDialog.show().ifPresent(tx -> {
            master.add(tx);
            service.add(tx);
        });
    }
    @FXML private void onRemove() {
        Transaction sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        if (new Alert(Alert.AlertType.CONFIRMATION,"Rimuovere la transazione?").showAndWait()
                .orElse(ButtonType.CANCEL)==ButtonType.OK) {
            master.remove(sel);
            service.replaceAll(master);
        }
    }

    /* filtri */
    @FXML public void applyFilters() {
        String kw = txtSearch.getText()==null ? "" : txtSearch.getText().toLowerCase();
        Category cat = cbFilterCat.getValue();
        TxType type  = cbFilterType.getValue();
        view.setPredicate(t ->
                (kw.isEmpty() || t.description().toLowerCase().contains(kw)) &&
                (cat==null || t.category()==cat) &&
                (type==null|| t.type()==type));
        refreshCharts(currentMonth());
    }

    /* mese */
    private YearMonth currentMonth() { return YearMonth.from(monthPicker.getValue()); }
    @FXML private void onMonthConfirm() { refreshCharts(currentMonth()); }

    /* grafici */
    private void refreshCharts(YearMonth ym) {
        var listMonth = master.stream()
                .filter(t -> YearMonth.from(t.date()).equals(ym))
                .collect(Collectors.toList());

        Money incomeSum = listMonth.stream()
                .filter(t -> t.type()==TxType.ENTRATA)
                .map(Transaction::amount).reduce(Money.ZERO, Money::add);
        Money expenseSum = listMonth.stream()
                .filter(t -> t.type()==TxType.USCITA)
                .map(Transaction::amount).reduce(Money.ZERO, Money::add);

        /* entrate per descrizione */
        Map<String,Money> byDescr = listMonth.stream()
                .filter(t -> t.type()==TxType.ENTRATA)
                .collect(Collectors.groupingBy(Transaction::description,
                        Collectors.reducing(Money.ZERO, Transaction::amount, Money::add)));
        ObservableList<PieChart.Data> inData = FXCollections.observableArrayList();
        byDescr.forEach((d,v)->inData.add(new PieChart.Data(d+" "+v,v.value().doubleValue())));
        pieIncome.setData(inData);
        pieIncome.setLegendVisible(false);

        /* uscite per categoria */
        Map<Category,Money> byCat = listMonth.stream()
                .filter(t -> t.type()==TxType.USCITA)
                .collect(Collectors.groupingBy(Transaction::category,
                        Collectors.reducing(Money.ZERO, Transaction::amount, Money::add)));
        ObservableList<PieChart.Data> outData = FXCollections.observableArrayList();
        byCat.forEach((c,v)->outData.add(new PieChart.Data(c.name()+" "+v,v.value().doubleValue())));
        pieExpense.setData(outData);
        pieExpense.setLegendVisible(false);

        lblSaldo.setText("Saldo: " + incomeSum.subtract(expenseSum));
    }

    /* export CSV */
    @FXML private void onExport() {
        FileChooser fc = new FileChooser();
        fc.setInitialFileName("money-"+currentMonth()+".csv");
        File f = fc.showSaveDialog(table.getScene().getWindow());
        if (f==null) return;
        try (PrintWriter pw = new PrintWriter(f, StandardCharsets.UTF_8)) {
            pw.println("data,tipo,categoria,importo,descrizione");
            view.forEach(t -> pw.printf("%s,%s,%s,%s,%s%n",
                    t.date(),t.type(),t.category(),t.amount(),t.description()));
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR,"Errore salvataggio CSV").showAndWait();
        }
    }
}
