/* ───── app/gui/MainController.java ───── */
package app.gui;

import app.dao.JsonTransactionDao;
import app.model.*;
import app.service.BudgetService;
import app.service.TransactionService;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class MainController {

    /* ─────────── FXML ─────────── */
    @FXML private TableView<Transaction> table;
    @FXML private TableColumn<Transaction,String> colDate,colType,colCat,colAmount,colDesc;
    @FXML private DatePicker monthPicker;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbFilterCat;
    @FXML private ComboBox<TxType> cbFilterType;
    @FXML private PieChart pieIncome,pieExpense;
    @FXML private Label lblSaldo;
    @FXML private Button btnEdit;
    @FXML private Button btnTrend;

    /* ─────────── costanti ─────────── */
    private static final Map<Category,String> CAT_COLOR = Map.of(
        Category.STIPENDIO, "#13e2c0ff",
        Category.AFFITTO, "#2196f3",
        Category.ALIMENTI, "#ffc107",
        Category.UTILITA, "#9c27b0",
        Category.INTRATTENIMENTO, "#f79400ff",
        Category.CARBURANTE, "#795548",
        Category.ALTRO, "#607d8b"
    );

    /* ───────── categorie personalizzate ───────── */
    public static final ObservableList<String> CUSTOM_CATEGORIES =
        FXCollections.observableArrayList();

    public static List<String> allCategoryNames() {
        List<String> list = Arrays.stream(Category.values()).map(Category::name).collect(Collectors.toCollection(ArrayList::new));
        list.addAll(CUSTOM_CATEGORIES);
        return list;
    }

    /* ─────────── stato ─────────── */
    private final TransactionService service = new TransactionService(new JsonTransactionDao(new File(System.getProperty("user.home"), ".money-minder.json")));
    private final BudgetService budgetSrv = new BudgetService();
    private final ObservableList<Transaction> master = FXCollections.observableArrayList();
    private final FilteredList<Transaction>  view   = new FilteredList<>(master, t -> true);
    private final Map<String,String> incomeColorCache = new HashMap<>();

    /* ─────────── init ─────────── */
    @FXML private void initialize() {
        bindColumns();
        styleCells();
        setupTable();
        setupFilters();

        master.setAll(service.list());
        monthPicker.setValue(LocalDate.now().withDayOfMonth(1));
        master.addListener((ListChangeListener<Transaction>) c -> refreshCharts(currentMonth()));
        
        applyFilters();
    }

    /* ─────────── toolbar actions ─────────── */
    @FXML private void onAdd() {
        TransactionDialog.show().ifPresent(tx -> {
            master.add(tx);
            service.add(tx);

            checkBudget(tx);
        });
    }
    @FXML private void onEdit() {
        Transaction orig = table.getSelectionModel().getSelectedItem();
        if (orig == null) return;

        TransactionDialog.show(orig).ifPresent(ed -> {
            int i = master.indexOf(orig);
            master.set(i, ed);
            service.replace(orig, ed);

            checkBudget(ed);
        });
    }

    @FXML private void onRemove() {
        Transaction sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        if (confirm("Rimuovere la transazione?")) {
            master.remove(sel);
            service.replaceAll(master);
        }
    }

    /* ─────────── budget ─────────── */
    private void checkBudget(Transaction tx) {
        if (tx.type() != TxType.USCITA) return;
        
        YearMonth ym = YearMonth.from(tx.date());

        Money speseMeseCat = master.stream().filter(t -> t.type() == TxType.USCITA
            && t.category().name().equals(tx.category().name())
            && YearMonth.from(t.date()).equals(ym)).map(Transaction::amount).reduce(Money.ZERO, Money::add);

        budgetSrv.get(tx.category().name()).ifPresent(budget -> {
            if (speseMeseCat.value().compareTo(budget.value()) > 0) {
                new Alert(Alert.AlertType.WARNING,
                        "⚠ Budget per " + tx.category().name() + " superato!\n" +
                        "Limite: " + budget + "   Spese: " + speseMeseCat)
                    .showAndWait();
            }
        });
    }

    /* ─────────── trend salary ─────────── */
    @FXML private void onTrend() {
        TrendDialog.show(
            (Stage) table.getScene().getWindow(),          // owner
            master                                         // tutte le transazioni attuali
        );
    }

    /* ─────────── filters ─────────── */
    @FXML private void applyFilters() {
        String kw = Optional.ofNullable(txtSearch.getText()).orElse("").toLowerCase();

        String catSel = cbFilterCat.getValue();
        TxType typeSel = cbFilterType.getValue();

        view.setPredicate(tr ->
            (kw.isBlank() || tr.description().toLowerCase().contains(kw)) &&
            (catSel == null || tr.category().name().equals(catSel)) &&
            (typeSel == null || tr.type() == typeSel));

        refreshCharts(currentMonth());
    }

    @FXML private void onMonthConfirm() { refreshCharts(currentMonth()); }

    /* ─────────── report ─────────── */
    @FXML private void onReport() {
        var ym  = YearMonth.from(monthPicker.getValue());
        var rep = service.monthlyReport(ym);

        MonthlyReportDialog.show((javafx.stage.Stage) table.getScene().getWindow(), ym, rep);
    }

    /* ─────────── opzioni ─────────── */
    @FXML private void onOptions() {
        BudgetDialog.show((Stage) table.getScene().getWindow(), allCategoryNames(), budgetSrv);
    }

    /* ─────────── grafici ─────────── */
    private void refreshCharts(YearMonth ym) {
        var month = master.stream().filter(t -> YearMonth.from(t.date()).equals(ym)).toList();

        Money totIn  = sum(month, TxType.ENTRATA);
        Money totOut = sum(month, TxType.USCITA);

        lblSaldo.setText("Saldo: " + totIn.subtract(totOut));

        pieIncome.setData(buildIncomeData(month));
        pieExpense.setData(buildExpenseData(month));
    }

    /* ─────────── export excel ─────────── */
    @FXML private void onExportXlsx() {
        FileChooser fc = new FileChooser();

        fc.setInitialFileName("MoneyMinder-" + currentMonth() + ".xlsx");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));

        File f = fc.showSaveDialog(table.getScene().getWindow());
        
        if (f == null) return;

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            var sh   = wb.createSheet("Transazioni");
            var bold = wb.createFont(); bold.setBold(true);
            var hdr  = wb.createCellStyle(); hdr.setFont(bold);
            var msty = wb.createCellStyle();

            msty.setDataFormat(wb.createDataFormat().getFormat("€#,##0.00"));

            String[] head = {"Data","Tipo","Categoria","Importo","Descrizione"};

            var r0 = sh.createRow(0);
            
            for (int i=0;i<head.length;i++) { var c=r0.createCell(i); c.setCellValue(head[i]); c.setCellStyle(hdr); }

            int r = 1;
            
            for (Transaction t : view) {
                var row = sh.createRow(r++);

                row.createCell(0).setCellValue(t.date().toString());
                row.createCell(1).setCellValue(t.type().toString());
                row.createCell(2).setCellValue(t.category().name());

                var cImp = row.createCell(3); cImp.setCellValue(t.amount().value().doubleValue()); cImp.setCellStyle(msty);
    
                row.createCell(4).setCellValue(t.description());
            }

            for (int i=0;i<head.length;i++) sh.autoSizeColumn(i);

            try (var out = new FileOutputStream(f)) { wb.write(out); }
        } catch (Exception ex) {
            alert("Errore durante l’esportazione:\n"+ex.getMessage());
        }
    }

    /* ─────────── helper ─────────── */
    private void bindColumns() {
        colDate  .setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().date().toString()));
        colType  .setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().type().toString()));
        colCat   .setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().category().name()));
        colAmount.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().amount().toString()));
        colDesc  .setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().description()));
    }
    private void styleCells() {
        colType.setCellFactory(tc -> colorCell(v -> v.equalsIgnoreCase("ENTRATA") ? "#4caf50" : "#e53935"));
        colCat .setCellFactory(tc -> colorCell(v -> CAT_COLOR.getOrDefault(Category.valueOf(v), "#aaaaaa")));
    }

    private TableCell<Transaction,String> colorCell(Function<String,String> fn) {
        return new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);

                if (empty || v == null) { setText(null); setStyle(""); return; }
                
                setText(v); setStyle("-fx-text-fill:"+fn.apply(v)+"; -fx-font-weight:bold;");
            }
        };
    }

    private void setupTable() {
        table.setItems(view);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setRowFactory(tv -> {
            var row = new TableRow<Transaction>() {
                @Override protected void updateItem(Transaction t, boolean empty) {
                    super.updateItem(t, empty);
                    getStyleClass().remove("card");
                    if (!empty) getStyleClass().add("card");
                }
            };
            row.setOnMouseClicked(e -> { if (e.getClickCount()==2 && !row.isEmpty()) onEdit(); });

            return row;
        });

        btnEdit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
    }
    private void setupFilters() {
        cbFilterCat.getItems().add(null); // entry “(tutte)”
        cbFilterCat.setItems(FXCollections.observableArrayList(allCategoryNames()));
        cbFilterType.getItems().add(null);
        cbFilterType.getItems().addAll(TxType.values());
    }

    private YearMonth currentMonth() { return YearMonth.from(monthPicker.getValue()); }

    private Money sum(List<Transaction> list, TxType t) {
        return list.stream().filter(tx -> tx.type()==t).map(Transaction::amount).reduce(Money.ZERO, Money::add);
    }

    private ObservableList<PieChart.Data> buildIncomeData(List<Transaction> month) {
        var map = month.stream().filter(t->t.type()==TxType.ENTRATA)
            .collect(Collectors.groupingBy(Transaction::description, Collectors
                .reducing(Money.ZERO, Transaction::amount, Money::add)));

        var data = FXCollections.<PieChart.Data>observableArrayList();
        
        map.forEach((d,v)->data.add(new PieChart.Data(d+" "+v, v.value().doubleValue())));
        colorSlices(data, d->incomeColor(d.getName()));

        return data;
    }

    private ObservableList<PieChart.Data> buildExpenseData(List<Transaction> month) {
        var map = month.stream().filter(t->t.type()==TxType.USCITA)
            .collect(Collectors.groupingBy(Transaction::category, Collectors.reducing(Money.ZERO, Transaction::amount, Money::add)));

        var data = FXCollections.<PieChart.Data>observableArrayList();

        map.forEach((c,v)->data.add(new PieChart.Data(c.name()+" "+v, v.value().doubleValue())));
        colorSlices(data, d->CAT_COLOR.getOrDefault(Category.valueOf(d.getName().split(" ")[0]),"#aaaaaa"));

        return data;
    }

    private void colorSlices(ObservableList<PieChart.Data> data, java.util.function.Function<PieChart.Data,String> fn) {
        javafx.application.Platform.runLater(() -> data.forEach(d -> {
            if (d.getNode() != null) { d.getNode().setStyle("-fx-pie-color:" + fn.apply(d) + ';'); }
        }));
    }

    private String incomeColor(String key) {
        return incomeColorCache.computeIfAbsent(key,k->{
            Color c = Color.hsb(120, 0.35 + (Math.abs(k.hashCode())%50)/100.0, 0.82);

            return String.format("#%02x%02x%02x", (int)(c.getRed()*255),(int)(c.getGreen()*255),(int)(c.getBlue()*255));
        });
    }

    private boolean confirm(String msg) {
        return new Alert(Alert.AlertType.CONFIRMATION,msg).showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK;
    }
    
    private void alert(String msg) { new Alert(Alert.AlertType.ERROR,msg).showAndWait(); }
}
