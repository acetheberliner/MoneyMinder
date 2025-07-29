/* ───────── app/gui/MainController.java ───────── */
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

    /* ─────────── FXML ─────────── */
    @FXML private TableView<Transaction> table;
    @FXML private TableColumn<Transaction,String> colDate,colType,colCat,colAmount,colDesc;
    @FXML private DatePicker monthPicker;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbFilterCat;
    @FXML private ComboBox<TxType> cbFilterType;
    @FXML private PieChart pieIncome,pieExpense;
    @FXML private Label lblTotIncome,lblTotExpense,lblSaldo;
    @FXML private Button btnEdit,btnTrend;

    /* ───── categorie custom ───── */
    public static final ObservableList<String> CUSTOM_CATEGORIES = FXCollections.observableArrayList();
    public static List<String> allCategoryNames() {
        var l = Arrays.stream(Category.values()).map(Category::name).collect(Collectors.toCollection(ArrayList::new));
        l.addAll(CUSTOM_CATEGORIES);
        return l;
    }

    /* ───── servizi & stato ───── */
    private final TransactionService service = new TransactionService(
        new JsonTransactionDao(new File(System.getProperty("user.home"), ".money-minder.json"))
    );
    private final BudgetService budgetSrv = new BudgetService();

    private final ObservableList<Transaction> master = FXCollections.observableArrayList();
    private final FilteredList<Transaction> filtered = new FilteredList<>(master, t -> true);
    private final SortedList<Transaction> sorted = new SortedList<>(filtered);

    private final Map<String,String> dynColor = new HashMap<>();   /* colori per categorie custom */
    private final Map<String,String> incomeColorCache = new HashMap<>();

    /* ───────── init ───────── */
    @FXML private void initialize() {
        bindColumns();
        styleCells();

        // carica tutte le transazioni pre esistenti
        master.setAll(service.list());

        // estrae dal DB eventuali categorie custom e le aggiunge alla lista 
        refreshCustomCatsFrom(master);

        // quand la lista è completa preparo table e filtri
        setupTable();
        setupFilters();

        var fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM");

        // converto la data da YYYY-MM-DD a YYYY-MM
        monthPicker.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(LocalDate d){
                return d == null ? "" : d.format(fmt);
            }
            @Override public LocalDate fromString(String s){
                if (s == null || s.isBlank()) return null;
                return YearMonth.parse(s, fmt).atDay(1);
            }
        });

        // impedisce la selezione di giorni diversi dal 1 del mese
        monthPicker.setDayCellFactory(dp -> new DateCell(){
            @Override public void updateItem(LocalDate d, boolean empty){
                super.updateItem(d, empty);
                if (!empty && d.getDayOfMonth() != 1) setDisable(true); // disabilita tutti tranne il giorno 1
            }
        });

        // default case: mese corrente
        monthPicker.setValue(LocalDate.now().withDayOfMonth(1));

        // se compaiono nuove transazioni con categorie custom le aggiungo ai filtri
        master.addListener((ListChangeListener<Transaction>) c -> {
            while (c.next() && c.wasAdded())
                refreshCustomCatsFrom(c.getAddedSubList());
            refreshCharts(currentMonth());
        });

        // primo render
        applyFilters();
    }


    // --------- Toolbar: azioni ---------

    // Aggiungi
    @FXML private void onAdd() {
        TransactionDialog.show().ifPresent(tx -> {
            master.add(tx);
            service.add(tx);
            checkBudget(tx);
        });
    }

    // Modifica
    @FXML private void onEdit() {
        Transaction sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        TransactionDialog.show(sel).ifPresent(ed -> {
            master.set(master.indexOf(sel), ed);
            service.replace(sel, ed);
            checkBudget(ed);
        });
    }

    // Rimuovi
    @FXML private void onRemove() {
        Transaction sel = table.getSelectionModel().getSelectedItem();
        if (sel != null && confirm("Rimuovere la transazione?")) {
           master.remove(sel);  service.replaceAll(master);
        }
    }
    // Aggiorna
    @FXML private void onUpdate() { applyFilters(); }

    // Selezione mese
    @FXML private void onMonthConfirm() { refreshCharts(currentMonth()); }
    
    // Andamento
    @FXML private void onTrend() {
        TrendDialog.show((Stage) table.getScene().getWindow(), master);
    }

    // Report
    @FXML private void onReport() {
        YearMonth ym = currentMonth();
        MonthlyReportDialog.show((Stage)table.getScene().getWindow(), ym, service.monthlyReport(ym));
    }

    // Budget
    @FXML private void onOptions() {
        BudgetDialog.show((Stage)table.getScene().getWindow(), allCategoryNames(), budgetSrv);
    }

    /* --------- Filtri --------- */
    @FXML private void applyFilters() {
        String kw = Optional.ofNullable(txtSearch.getText()).orElse("").toLowerCase().trim();
        String cat = cbFilterCat.getValue();
        TxType tSel = cbFilterType.getValue();
        YearMonth ym = currentMonth();

        filtered.setPredicate(tr ->
            (kw.isBlank() || tr.description().toLowerCase().contains(kw)) &&
            (cat == null || cat.isBlank() || tr.categoryName().equals(cat)) &&
            (tSel == null || tr.type() == tSel) &&
            YearMonth.from(tr.date()).equals(ym)
        );

        refreshCharts(ym);
    }

    /* --------- Budget check --------- */
    private void checkBudget(Transaction tx) {
        if (tx.type() != TxType.USCITA) return;

        YearMonth ym = YearMonth.from(tx.date());
        Money spent = master.stream()
            .filter(t -> t.type()==TxType.USCITA
                && t.categoryName().equals(tx.categoryName())
                && YearMonth.from(t.date()).equals(ym))
            .map(Transaction::amount)
            .reduce(Money.ZERO, Money::add);

        budgetSrv.get(tx.categoryName()).ifPresent(limit -> {
            if (spent.value().compareTo(limit.value()) > 0) {
                new Alert(Alert.AlertType.WARNING,
                    "Budget per "+tx.categoryName()+" superato!\n"+
                    "Limite: "+limit+"   Spese: "+spent).showAndWait();
            }
        });
    }

    /* --------- Grafici --------- */
    private void refreshCharts(YearMonth ym) {
        var month = master.stream().filter(t -> YearMonth.from(t.date()).equals(ym)).toList();

        Money in  = sum(month, TxType.ENTRATA);
        Money out = sum(month, TxType.USCITA);

        lblTotIncome.setText("Entrate: "+in);
        lblTotExpense.setText("Uscite: "+out);
        lblSaldo.setText("Saldo: "+in.subtract(out));

        pieIncome.setData(buildIncomeData(month));
        pieIncome.setLegendVisible(false);
        
        pieExpense.setData(buildExpenseData(month));
        pieExpense.setLegendVisible(false);
    }

    // Refresh delle categorie custom
    private void refreshCustomCatsFrom(List<? extends Transaction> src) {
        src.stream().map(Transaction::categoryName).filter(name -> {
            try { Category.valueOf(name); return false; }
            catch (Exception ex) { return true; }
        })
        .filter(name -> !CUSTOM_CATEGORIES.contains(name))
        .forEach(CUSTOM_CATEGORIES::add);
    }

    /* --------- Export Excel --------- */
    @FXML private void onExportXlsx() {
        FileChooser fc = new FileChooser();

        fc.setInitialFileName("MoneyMinder-"+currentMonth()+".xlsx");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel (*.xlsx)","*.xlsx")
        );

        File f = fc.showSaveDialog(table.getScene().getWindow());
        
        if (f == null) return;

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            var sh = wb.createSheet("Transazioni");
            var bold = wb.createFont(); bold.setBold(true);
            var hdr  = wb.createCellStyle(); hdr.setFont(bold);
            var money= wb.createCellStyle();
        
            money.setDataFormat(wb.createDataFormat().getFormat("€#,##0.00"));

            String[] head = {"Data","Tipo","Categoria","Importo","Descrizione"};
            var r0 = sh.createRow(0);
            for (int i=0;i<head.length;i++){
                var c=r0.createCell(i); c.setCellValue(head[i]); c.setCellStyle(hdr);
            }

            int r = 1;
        
            for (Transaction t : filtered){
                var row = sh.createRow(r++);
        
                row.createCell(0).setCellValue(t.date().toString());
                row.createCell(1).setCellValue(t.type().toString());
                row.createCell(2).setCellValue(t.categoryName());
        
                var cImp=row.createCell(3);
        
                cImp.setCellValue(t.amount().value().doubleValue());
                cImp.setCellStyle(money);
                row.createCell(4).setCellValue(t.description());
            }
            for(int i=0;i<head.length;i++) sh.autoSizeColumn(i);
            try(FileOutputStream out = new FileOutputStream(f)){ wb.write(out); }
        }catch(Exception ex){ alert("Errore export:\n"+ex.getMessage()); }
    }

    /* --------- Tabella Storico --------- */
    private void setupTable(){
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);

        colDate.setSortType(TableColumn.SortType.ASCENDING);
        table.getSortOrder().setAll(colDate);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setRowFactory(tv -> {
            TableRow<Transaction> row = new TableRow<>(){
                @Override protected void updateItem(Transaction t, boolean empty){
                    super.updateItem(t, empty);

                    getStyleClass().remove("card");
                    if(!empty) getStyleClass().add("card");
                }
            };
            row.setOnMouseClicked(e -> {
                if(e.getClickCount()==2 && !row.isEmpty()) onEdit();
            });

            return row;
        });
        btnEdit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
    }

    /* --------- Colonne storico --------- */
    private void bindColumns(){
        colDate.setCellValueFactory(c->new ReadOnlyStringWrapper(c.getValue().date().toString()));
        colType.setCellValueFactory(c->new ReadOnlyStringWrapper(c.getValue().type().toString()));
        colCat.setCellValueFactory(c->new ReadOnlyStringWrapper(c.getValue().categoryName()));
        colAmount.setCellValueFactory(c->new ReadOnlyStringWrapper(c.getValue().amount().toString()));
        colDesc.setCellValueFactory(c->new ReadOnlyStringWrapper(c.getValue().description()));
    }

    /* --------- Colore per categoria --------- */
    private static final Map<Category, String> CAT_COLOR = Map.of(
        Category.STIPENDIO, "#13e2c0",
        Category.AFFITTO, "#2196f3",
        Category.ALIMENTI, "#ffc107",
        Category.UTILITA, "#9c27b0",
        Category.INTRATTENIMENTO, "f79400",
        Category.CARBURANTE, "#795548",
        Category.ALTRO, "#607d8b"
    );

    // --------- Stile celle dei piechart ---------
    private void styleCells() {
        // colore verde/rosso sulla tipologia
        colType.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null){
                    setText(null);
                    setStyle("");
                    return; 
                }
                setText(v);
                setStyle("-fx-text-fill:" + (v.equalsIgnoreCase("ENTRATA") ? "#4caf50" : "#e53935") + "; -fx-font-weight:bold;");
            }
        });

        /* piechart uscite: colore in base alla categoria (mappa CAT_COLOR) */
        colCat.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null){
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(v);
                
                String col = CAT_COLOR.getOrDefault(
                    /* se è enum lo trova al volo, altrimenti usa grigio */
                    Arrays.stream(Category.values()).map(Category::name).anyMatch(v::equals) ? Category.valueOf(v) : Category.ALTRO, "#aaaaaa");
                setStyle("-fx-text-fill:"+col+"; -fx-font-weight:bold;");
            }
        });
    }

    /* --------- Setup Filtri --------- */
    private void setupFilters() {

        // popolamento iniziale
        refreshFilterCategories();

        /* Tipologia Transazione */
        cbFilterType.getItems().setAll((TxType) null);
        cbFilterType.getItems().addAll(TxType.values());
        cbFilterType.setPromptText("Tipologia");

        // aggiungendo/rimuovendo categorie custom aggiorno la tendina dei filtri
        CUSTOM_CATEGORIES.addListener((ListChangeListener<String>) c -> refreshFilterCategories());
    }

    // ricreo le voci della tendina categorie 
    private void refreshFilterCategories() {
        /* salva l’eventuale selezione corrente */
        String sel = cbFilterCat.getValue();

        cbFilterCat.getItems().setAll("Categoria");
        cbFilterCat.getItems().addAll(allCategoryNames());

        /* ristabilisci la selezione se ancora presente */
        if (cbFilterCat.getItems().contains(sel)) {
            cbFilterCat.setValue(sel);
        } else {
            cbFilterCat.setValue("");
        }
    }

    /* --------- Chart helpers --------- */
    private YearMonth currentMonth(){ 
        return YearMonth.from(monthPicker.getValue()); 
    }

    private Money sum(List<Transaction> list, TxType t){
        return list.stream().filter(x->x.type()==t).map(Transaction::amount).reduce(Money.ZERO, Money::add);
    }

    // Grafico delle entrate
    private ObservableList<PieChart.Data> buildIncomeData(List<Transaction> month){
        var map = month.stream().filter(t->t.type()==TxType.ENTRATA)
                       .collect(Collectors.groupingBy(Transaction::description, Collectors.reducing(Money.ZERO, Transaction::amount, Money::add)));

        var data = FXCollections.<PieChart.Data>observableArrayList();

        map.forEach((d,v)->data.add(new PieChart.Data(d+" "+v, v.value().doubleValue())));

        colorSlices(data, d->incomeColor(d.getName()));
        return data;
    }

    // Grafico delle uscite
    private ObservableList<PieChart.Data> buildExpenseData(List<Transaction> month){
        var map = month.stream().filter(t->t.type()==TxType.USCITA)
                       .collect(Collectors.groupingBy(Transaction::categoryName, Collectors.reducing(Money.ZERO, Transaction::amount, Money::add)));

        var data = FXCollections.<PieChart.Data>observableArrayList();

        map.forEach((c,v)->data.add(new PieChart.Data(c+" "+v, v.value().doubleValue())));
        colorSlices(data, d->categoryColor(d.getName().split(" ")[0]));

        return data;
    }

    // Slice dei piechart con colore dinamico
    private void colorSlices(ObservableList<PieChart.Data> data, Function<PieChart.Data,String> fn){
        javafx.application.Platform.runLater(() ->
        data.forEach(d -> {
            if(d.getNode()!=null) d.getNode().setStyle("-fx-pie-color:" + fn.apply(d));
        }));
    }

    // colore per categoria (preset o custom)
    private String categoryColor(String name){
        try { return CAT_COLOR.get(Category.valueOf(name)); }

        catch (Exception ex){
            /* categoria custom → deterministico su hash */
            return dynColor.computeIfAbsent(name, k -> {
                Color c = Color.hsb((k.hashCode() & 0xffff)%360, 0.55, 0.75);

                return String.format("#%02x%02x%02x", (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
            });
        }
    }

    // colore per singola entrata
    private String incomeColor(String key){
        return incomeColorCache.computeIfAbsent(key, k -> {
            // tonalità nel verde + saturazione variabile
            Color c = Color.hsb(120, 0.35 + (Math.abs(k.hashCode()) % 50) / 100.0, 0.82);
            return String.format("#%02x%02x%02x", (int)(c.getRed()  * 255), (int)(c.getGreen()* 255), (int)(c.getBlue() * 255));
        });
    }

    /* --------- confirmation dialog --------- */
    private boolean confirm(String msg){
        return new Alert(Alert.AlertType.CONFIRMATION,msg).showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK;
    }
    private void alert(String msg){ new Alert(Alert.AlertType.ERROR,msg).showAndWait(); }
}
