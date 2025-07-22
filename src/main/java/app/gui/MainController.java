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
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

public final class MainController {

    @FXML private TableView<Transaction> table;
    @FXML private TableColumn<Transaction,String> colDate,colType,colCat,colAmount,colDesc;
    @FXML private DatePicker monthPicker;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<Category> cbFilterCat;
    @FXML private ComboBox<TxType>   cbFilterType;
    @FXML private PieChart pieIncome,pieExpense;
    @FXML private Label lblSaldo;
    @FXML private Button btnEdit;

    @FXML private void onEdit() {
        Transaction orig = table.getSelectionModel().getSelectedItem();
        if (orig == null) return;

        TransactionDialog.show(orig).ifPresent(edited -> {
            int idx = master.indexOf(orig);
            master.set(idx, edited);              // sostituisce in tabella
            service.replace(orig, edited);        // salva su disco
        });
    }

    private final TransactionService service = new TransactionService(
            new JsonTransactionDao(new File(System.getProperty("user.home"), ".money-minder.json")));
    private final ObservableList<Transaction> master = FXCollections.observableArrayList();
    private final FilteredList<Transaction>  view   = new FilteredList<>(master, t -> true);

    private static final Map<Category,String> CAT_COLOR = Map.of(
            Category.STIPENDIO,       "#13e2c0ff",
            Category.AFFITTO,         "#2196f3",
            Category.ALIMENTI,        "#ffc107",
            Category.UTILITA,         "#9c27b0",
            Category.INTRATTENIMENTO, "#f79400ff",
            Category.CARBURANTE,      "#795548",
            Category.ALTRO,           "#607d8b"
    );
    private final Map<String,String> incomeColorCache = new HashMap<>();

    @FXML
    private void initialize() {
        /* column bindings */
        colDate  .setCellValueFactory(c->new ReadOnlyStringWrapper(c.getValue().date().toString()));
        colType  .setCellValueFactory(c->new ReadOnlyStringWrapper(c.getValue().type().toString()));
        colCat   .setCellValueFactory(c->new ReadOnlyStringWrapper(c.getValue().category().name()));
        colAmount.setCellValueFactory(c->new ReadOnlyStringWrapper(c.getValue().amount().toString()));
        colDesc  .setCellValueFactory(c->new ReadOnlyStringWrapper(c.getValue().description()));

        /* cell colors (text only) */
        colType.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v==null) { setText(null); setStyle(""); return; }
                setText(v);
                String col = v.equalsIgnoreCase("ENTRATA") ? "#4caf50" : "#e53935";
                setStyle("-fx-text-fill:"+col+"; -fx-font-weight:bold;");
            }
        });
        colCat.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v==null) { setText(null); setStyle(""); return; }
                setText(v);
                String col = CAT_COLOR.getOrDefault(Category.valueOf(v), "#aaaaaa");
                setStyle("-fx-text-fill:"+col+"; -fx-font-weight:bold;");
            }
        });

        master.setAll(service.list());
        table.setItems(view);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Transaction t, boolean empty) {
                super.updateItem(t, empty);
                getStyleClass().remove("card");          // reset
                if (!empty) getStyleClass().add("card"); // applica stile card
            }
        });

        table.setRowFactory(tv -> {
            TableRow<Transaction> row = new TableRow<>() {
                @Override protected void updateItem(Transaction t, boolean empty) {
                    super.updateItem(t, empty);
                    getStyleClass().remove("card");
                    if (!empty) getStyleClass().add("card");
                }
            };
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) onEdit();
            });
            return row;
        });

        btnEdit.disableProperty().bind(
        table.getSelectionModel().selectedItemProperty().isNull());

        cbFilterCat.getItems().add(null);
        cbFilterCat.getItems().addAll(Category.values());
        cbFilterType.getItems().add(null);
        cbFilterType.getItems().addAll(TxType.values());

        monthPicker.setValue(LocalDate.now().withDayOfMonth(1));

        master.addListener((ListChangeListener<Transaction>) c -> refreshCharts(currentMonth()));
        applyFilters();
    }

    /* add / remove */
    @FXML private void onAdd() {
        TransactionDialog.show().ifPresent(tx -> { master.add(tx); service.add(tx); });
    }
    @FXML private void onRemove() {
        Transaction sel = table.getSelectionModel().getSelectedItem();
        if (sel==null) return;
        if (new Alert(Alert.AlertType.CONFIRMATION,"Rimuovere la transazione?")
                .showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK) {
            master.remove(sel); service.replaceAll(master);
        }
    }

    /* filtri */
    @FXML public void applyFilters() {
        String kw = txtSearch.getText()==null?"" : txtSearch.getText().toLowerCase();
        Category c = cbFilterCat.getValue(); TxType t = cbFilterType.getValue();
        view.setPredicate(tr ->
             (kw.isEmpty() || tr.description().toLowerCase().contains(kw)) &&
             (c==null || tr.category()==c) &&
             (t==null || tr.type()==t));
        refreshCharts(currentMonth());
    }

    /* mese */
    private YearMonth currentMonth(){ return YearMonth.from(monthPicker.getValue()); }
    @FXML private void onMonthConfirm(){ refreshCharts(currentMonth()); }

    /* grafici */
    private void refreshCharts(YearMonth ym) {
        List<Transaction> month = master.stream()
                .filter(t -> YearMonth.from(t.date()).equals(ym)).toList();

        Money totIn = month.stream().filter(t->t.type()==TxType.ENTRATA)
                .map(Transaction::amount).reduce(Money.ZERO, Money::add);
        Money totOut= month.stream().filter(t->t.type()==TxType.USCITA)
                .map(Transaction::amount).reduce(Money.ZERO, Money::add);

        Map<String,Money> inMap = month.stream().filter(t->t.type()==TxType.ENTRATA)
                .collect(Collectors.groupingBy(Transaction::description,
                        Collectors.reducing(Money.ZERO, Transaction::amount, Money::add)));
        ObservableList<PieChart.Data> inData = FXCollections.observableArrayList();
        inMap.forEach((d,v)->inData.add(new PieChart.Data(d+" "+v,v.value().doubleValue())));
        pieIncome.setData(inData); pieIncome.setLegendVisible(false);
        colorSlices(inData, d->incomeColor(d.getName()));

        Map<Category,Money> outMap = month.stream().filter(t->t.type()==TxType.USCITA)
                .collect(Collectors.groupingBy(Transaction::category,
                        Collectors.reducing(Money.ZERO, Transaction::amount, Money::add)));
        ObservableList<PieChart.Data> outData = FXCollections.observableArrayList();
        outMap.forEach((cat,v)->outData.add(new PieChart.Data(cat.name()+" "+v,v.value().doubleValue())));
        pieExpense.setData(outData); pieExpense.setLegendVisible(false);
        colorSlices(outData, d->{
            String k=d.getName().split(" ")[0];
            return CAT_COLOR.getOrDefault(Category.valueOf(k),"#aaaaaa");
        });

        lblSaldo.setText("Saldo: " + totIn.subtract(totOut));
    }
    private void colorSlices(ObservableList<PieChart.Data> data,
                             java.util.function.Function<PieChart.Data,String> fn){
        data.forEach(d->d.getNode().setStyle("-fx-pie-color:"+fn.apply(d)+";"));
    }
    private String incomeColor(String key){
        return incomeColorCache.computeIfAbsent(key,k->{
            int h=Math.abs(k.hashCode()); double sat=0.35+(h%50)/100.0;
            Color c=Color.hsb(120,sat,0.82);
            return String.format("#%02x%02x%02x",
                    (int)(c.getRed()*255),(int)(c.getGreen()*255),(int)(c.getBlue()*255));
        });
    }

    /* export */
    @FXML private void onExportXlsx() {
        FileChooser fc = new FileChooser();
        fc.setInitialFileName("MoneyMinder-" + currentMonth() + ".xlsx");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        File file = fc.showSaveDialog(table.getScene().getWindow());
        if (file == null) return;

        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {

            var sheet = wb.createSheet("Transazioni");

            /* ---- stili ---- */
            var headerFont = wb.createFont();
            headerFont.setBold(true);
            var headerStyle = wb.createCellStyle();
            headerStyle.setFont(headerFont);

            var moneyStyle = wb.createCellStyle();
            var fmt = wb.createDataFormat();
            moneyStyle.setDataFormat(fmt.getFormat("€#,##0.00"));

            /* ---- intestazioni ---- */
            String[] cols = {"Data", "Tipo", "Categoria", "Importo", "Descrizione"};
            var row0 = sheet.createRow(0);
            for (int i = 0; i < cols.length; i++) {
                var c = row0.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
            }

            /* ---- righe ---- */
            int r = 1;
            for (Transaction t : view) {          // solo righe filtrate
                var row = sheet.createRow(r++);
                row.createCell(0).setCellValue(t.date().toString());
                row.createCell(1).setCellValue(t.type().toString());
                row.createCell(2).setCellValue(t.category().name());

                var cImporto = row.createCell(3);
                cImporto.setCellValue(t.amount().value().doubleValue());
                cImporto.setCellStyle(moneyStyle);

                row.createCell(4).setCellValue(t.description());
            }

            /* larghezza automatica colonne */
            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);

            /* salva su disco */
            try (var out = new java.io.FileOutputStream(file)) { wb.write(out); }

        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR,
                    "Errore durante l’esportazione Excel:\n" + ex.getMessage())
                .showAndWait();
        }
    }
}
