/* ───── app/gui/MainController.java ───── */
package app.gui;

import app.dao.JsonTransactionDao;
import app.model.*;
import app.service.BudgetService;
import app.service.TransactionService;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class MainController {

    /* ──────── FXML ──────── */
    @FXML private TableView<Transaction> table;
    @FXML private TableColumn<Transaction,String> colDate,colType,colCat,colAmount,colDesc;
    @FXML private DatePicker  monthPicker;
    @FXML private TextField   txtSearch;
    @FXML private ComboBox<String> cbFilterCat;
    @FXML private ComboBox<TxType> cbFilterType;
    @FXML private PieChart   pieIncome, pieExpense;
    @FXML private Label      lblTotIncome, lblTotExpense, lblSaldo;
    @FXML private Button     btnEdit, btnTrend;

    /* ──────── colori pre-definiti ──────── */
    private static final Map<Category,String> CAT_COLOR = Map.of(
            Category.STIPENDIO,       "#13e2c0",
            Category.AFFITTO,         "#2196f3",
            Category.ALIMENTI,        "#ffc107",
            Category.UTILITA,         "#9c27b0",
            Category.INTRATTENIMENTO, "#f79400",
            Category.CARBURANTE,      "#795548",
            Category.ALTRO,           "#607d8b"
    );

    /* ───── categorie custom (in RAM) ───── */
    public static final ObservableList<String> CUSTOM_CATEGORIES = FXCollections.observableArrayList();
    public static List<String> allCategoryNames() {
        var l = Arrays.stream(Category.values()).map(Category::name).collect(Collectors.toCollection(ArrayList::new));
        l.addAll(CUSTOM_CATEGORIES);
        return l;
    }

    /* ──────── servizi / stato ──────── */
    private final TransactionService service = new TransactionService(
            new JsonTransactionDao(new File(System.getProperty("user.home"), ".money-minder.json")));
    private final BudgetService budgetSrv   = new BudgetService();

    private final ObservableList<Transaction> master   = FXCollections.observableArrayList();
    private final FilteredList<Transaction>   filtered = new FilteredList<>(master, t -> true);
    private final SortedList<Transaction>     sorted   = new SortedList<>(filtered);

    private final Map<String,String> incomeColorCache = new HashMap<>();

    /* ──────── init ──────── */
    @FXML private void initialize() {
        bindColumns();
        styleCells();
        setupTable();
        setupFilters();

        master.setAll(service.list());

        monthPicker.setValue(LocalDate.now().withDayOfMonth(1));
        monthPicker.valueProperty().addListener((o, ov, nv) -> applyFilters()); // ← reagisce al cambio mese

        master.addListener((ListChangeListener<Transaction>) c -> refreshCharts(currentMonth()));

        applyFilters();   // primissimo caricamento
    }

    /* ──────── toolbar ──────── */
    @FXML private void onAdd() {
        TransactionDialog.show().ifPresent(tx -> {
            master.add(tx);    // aggiorna tabella
            service.add(tx);   // persiste
            checkBudget(tx);
        });
    }
    @FXML private void onEdit() {
        Transaction orig = table.getSelectionModel().getSelectedItem();
        if (orig == null) return;

        TransactionDialog.show(orig).ifPresent(ed -> {
            master.set(master.indexOf(orig), ed);
            service.replace(orig, ed);
            checkBudget(ed);
        });
    }
    @FXML private void onRemove() {
        Transaction sel = table.getSelectionModel().getSelectedItem();
        if (sel != null && confirm("Rimuovere la transazione?")) {
            master.remove(sel);
            service.replaceAll(master);
        }
    }

    @FXML private void onUpdate() {
        applyFilters();
    }

    @FXML private void onMonthConfirm() {           // ← SENZA parametri
        refreshCharts(currentMonth());
    }

    /* ──────── budget check ──────── */
    private void checkBudget(Transaction tx) {
        if (tx.type() != TxType.USCITA) return;

        YearMonth ym = YearMonth.from(tx.date());
        Money spese = master.stream()
                .filter(t -> t.type()==TxType.USCITA
                          && t.category()==tx.category()
                          && YearMonth.from(t.date()).equals(ym))
                .map(Transaction::amount)
                .reduce(Money.ZERO, Money::add);

        budgetSrv.get(tx.category().name()).ifPresent(limit -> {
            if (spese.value().compareTo(limit.value()) > 0) {
                new Alert(Alert.AlertType.WARNING,
                        "Budget per "+tx.category().name()+" superato!\n"+
                        "Limite: "+limit+"  Spese: "+spese).showAndWait();
            }
        });
    }

    /* ──────── trend chart ──────── */
    @FXML private void onTrend() {
        TrendDialog.show((Stage) table.getScene().getWindow(), master);
    }

    /* ──────── filtri ──────── */
    // @FXML private void applyFilters() {
    //     String kw  = Optional.ofNullable(txtSearch.getText()).orElse("").toLowerCase();
    //     String cat = cbFilterCat.getValue();
    //     TxType ty  = cbFilterType.getValue();
    //     YearMonth selectedMonth = YearMonth.from(monthPicker.getValue());

    //     filtered.setPredicate(tr ->
    //             (kw.isBlank() || tr.description().toLowerCase().contains(kw)) &&
    //             (cat == null  || tr.category().name().equals(cat))            &&
    //             (ty  == null  || tr.type()==ty)                               &&
    //             YearMonth.from(tr.date()).equals(selectedMonth));

    //     refreshCharts(selectedMonth);
    // }
    @FXML private void applyFilters() {
        String kw = Optional.ofNullable(txtSearch.getText())
                            .orElse("")
                            .toLowerCase()
                            .trim();

        String catSel = cbFilterCat.getValue();   /* può essere "" */
        TxType  tSel  = cbFilterType.getValue();

        filtered.setPredicate(tr ->
            (kw.isBlank() || tr.description().toLowerCase().contains(kw)) &&
            (catSel == null || catSel.isBlank() || tr.category().name().equals(catSel)) &&
            (tSel  == null || tr.type() == tSel)
        );

        refreshCharts(currentMonth());
    }

    /* ──────── report ──────── */
    @FXML private void onReport() {
        YearMonth ym = currentMonth();
        MonthlyReportDialog.show((Stage) table.getScene().getWindow(), ym, service.monthlyReport(ym));
    }

    /* ──────── opzioni budget ──────── */
    @FXML private void onOptions() {
        BudgetDialog.show((Stage) table.getScene().getWindow(), allCategoryNames(), budgetSrv);
    }

    /* ──────── grafici ──────── */
    private void refreshCharts(YearMonth ym) {
        var month = master.stream().filter(t -> YearMonth.from(t.date()).equals(ym)).toList();

        Money totIn  = sum(month, TxType.ENTRATA);
        Money totOut = sum(month, TxType.USCITA);

        lblTotIncome.setText("Entrate: "+totIn);
        lblTotExpense.setText("Uscite: "+totOut);
        lblSaldo.setText("Saldo: "+totIn.subtract(totOut));

        pieIncome.setData(buildIncomeData(month));
        pieExpense.setData(buildExpenseData(month));

        pieIncome.setLegendVisible(false);
        pieExpense.setLegendVisible(false);
    }

    /* ──────── Excel export ──────── */
    @FXML private void onExportXlsx() {
        FileChooser fc = new FileChooser();
        fc.setInitialFileName("MoneyMinder-"+currentMonth()+".xlsx");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        File f = fc.showSaveDialog(table.getScene().getWindow());
        if (f == null) return;

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            var sh   = wb.createSheet("Transazioni");
            var bold = wb.createFont(); bold.setBold(true);
            var hdr  = wb.createCellStyle(); hdr.setFont(bold);
            var money= wb.createCellStyle(); money.setDataFormat(wb.createDataFormat().getFormat("€#,##0.00"));

            String[] head = {"Data","Tipo","Categoria","Importo","Descrizione"};
            var r0 = sh.createRow(0);
            for (int i=0;i<head.length;i++){ var c=r0.createCell(i); c.setCellValue(head[i]); c.setCellStyle(hdr); }

            int r=1;
            for (Transaction t: filtered){ // solo quelle visibili
                var row = sh.createRow(r++);
                row.createCell(0).setCellValue(t.date().toString());
                row.createCell(1).setCellValue(t.type().toString());
                row.createCell(2).setCellValue(t.category().name());
                var cImp=row.createCell(3); cImp.setCellValue(t.amount().value().doubleValue()); cImp.setCellStyle(money);
                row.createCell(4).setCellValue(t.description());
            }
            for(int i=0;i<head.length;i++) sh.autoSizeColumn(i);
            try(var out=new FileOutputStream(f)){ wb.write(out); }
        }catch(Exception ex){ alert("Errore export:\n"+ex.getMessage()); }
    }

    /* ──────── configurazioni tabella ──────── */
    private void setupTable() {
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);

        // default: data crescente
        colDate.setSortType(TableColumn.SortType.ASCENDING);
        table.getSortOrder().setAll(Collections.singletonList(colDate));

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

    /* ──────── colonne & stile ──────── */
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
    private TableCell<Transaction,String> colorCell(Function<String,String> fn){
        return new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v==null){ setText(null); setStyle(""); return; }
                setText(v); setStyle("-fx-text-fill:"+fn.apply(v)+"; -fx-font-weight:bold;");
            }
        };
    }

    /* ──────── filtri iniziali ──────── */
    // private void setupFilters() {
    //     cbFilterCat.getItems().add(null);                 // “tutte”
    //     cbFilterCat.setItems(FXCollections.observableArrayList(allCategoryNames()));
    //     cbFilterType.getItems().add(null);
    //     cbFilterType.getItems().addAll(TxType.values());
    // }
    private void setupFilters() {
        /* (1)  voce “(tutte)” – usiamo stringa vuota, il prompt la mostra */
        cbFilterCat.getItems().setAll("");                   // reset
        cbFilterCat.getItems().addAll(allCategoryNames());   // enum + custom
        cbFilterCat.setPromptText("Categoria");

        /* (2)  TxType */
        cbFilterType.getItems().setAll((TxType) null);       // reset
        cbFilterType.getItems().addAll(TxType.values());
        cbFilterType.setPromptText("Tipo");
    }

    /* ──────── utilità ──────── */
    private YearMonth currentMonth(){ return YearMonth.from(monthPicker.getValue()); }
    private Money sum(List<Transaction> l, TxType t){ return l.stream().filter(x->x.type()==t).map(Transaction::amount).reduce(Money.ZERO, Money::add); }

    private ObservableList<PieChart.Data> buildIncomeData(List<Transaction> month){
        var map = month.stream().filter(t->t.type()==TxType.ENTRATA)
                .collect(Collectors.groupingBy(Transaction::description,
                        Collectors.reducing(Money.ZERO, Transaction::amount, Money::add)));

        var data = FXCollections.<PieChart.Data>observableArrayList();
        map.forEach((d,v)->data.add(new PieChart.Data(d+" "+v, v.value().doubleValue())));
        colorSlices(data, d->incomeColor(d.getName()));
        return data;
    }
    private ObservableList<PieChart.Data> buildExpenseData(List<Transaction> month){
        var map = month.stream().filter(t->t.type()==TxType.USCITA)
                .collect(Collectors.groupingBy(Transaction::category,
                        Collectors.reducing(Money.ZERO, Transaction::amount, Money::add)));

        var data = FXCollections.<PieChart.Data>observableArrayList();
        map.forEach((c,v)->data.add(new PieChart.Data(c.name()+" "+v, v.value().doubleValue())));
        colorSlices(data, d->CAT_COLOR.getOrDefault(Category.valueOf(d.getName().split(" ")[0]), "#aaaaaa"));
        return data;
    }
    private void colorSlices(ObservableList<PieChart.Data> data, Function<PieChart.Data,String> fn){
        javafx.application.Platform.runLater(() ->
            data.forEach(d -> { if (d.getNode()!=null) d.getNode().setStyle("-fx-pie-color:"+fn.apply(d)); }));
    }
    private String incomeColor(String k){
        return incomeColorCache.computeIfAbsent(k, key -> {
            Color c = Color.hsb(120, 0.35+(Math.abs(key.hashCode())%50)/100.0, 0.82);
            return String.format("#%02x%02x%02x", (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
        });
    }
    private boolean confirm(String msg){ return new Alert(Alert.AlertType.CONFIRMATION,msg).showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK; }
    private void alert(String msg){ new Alert(Alert.AlertType.ERROR,msg).showAndWait(); }
}
