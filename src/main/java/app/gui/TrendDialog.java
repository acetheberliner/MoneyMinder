package app.gui;

import app.model.Money;
import app.model.Transaction;
import app.model.TxType;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.text.NumberFormat;
import java.time.Year;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public final class TrendDialog {
    private enum Gran { Giornaliero, Mensile, Annuale }

    private static final String COLOR = "#2196f3";
    private static final NumberFormat EUR = NumberFormat.getCurrencyInstance(Locale.ITALY);

    public static void show(Stage owner, List<Transaction> txs) {
        ChoiceBox<Gran> cb = new ChoiceBox<>(FXCollections.observableArrayList(Gran.values()));
        cb.getSelectionModel().select(Gran.Giornaliero);

        NumberAxis y = new NumberAxis();
        CategoryAxis x = new CategoryAxis();

        LineChart<String, Number> chart = new LineChart<>(x, y);
        chart.setAnimated(false);
        chart.setLegendVisible(false);

        cb.getSelectionModel().selectedItemProperty().addListener((o, old, g) -> refresh(chart, txs, g));

        BorderPane root = new BorderPane(chart);
        root.setTop(cb);
        BorderPane.setMargin(cb, new Insets(6));

        Stage st = new Stage();
        
        st.initOwner(owner);
        st.initModality(Modality.WINDOW_MODAL);
        st.setTitle("Andamento saldo");
        st.setScene(new Scene(root, 850, 420));

        refresh(chart, txs, Gran.Giornaliero);

        st.show();
    }

    /* ----------------- helper ----------------- */
    private static void refresh(LineChart<String, Number> chart, List<Transaction> txs, Gran g) {
        XYChart.Series<String, Number> s = buildSeries(txs, g);
        chart.setData(FXCollections.observableList(Collections.singletonList(s)));
        chart.setTitle("Andamento saldo - " + g.name());

        // stile linea + marker azzurro
        Platform.runLater(() -> {
        s.getNode().setStyle("-fx-stroke: " + COLOR + "; -fx-stroke-width: 2px;");

        for (XYChart.Data<String, Number> d : s.getData()) {
            if (d.getNode() == null) continue;

            /* colore marker */
            d.getNode().setStyle("-fx-background-color: " + COLOR + ", white;");

            /* tooltip (già presente, ma lo teniamo) */
            Tooltip.install(d.getNode(),
                    new Tooltip(EUR.format(d.getYValue().doubleValue())));

            /* ★ etichetta sempre visibile ------------------------ */
            var label = new javafx.scene.control.Label(
                    EUR.format(d.getYValue().doubleValue()));
            label.setStyle("-fx-font-size: 10; -fx-text-fill: #555;");

            // centra la label sopra il punto
            label.layoutXProperty().bind(d.getNode().layoutXProperty().subtract(label.widthProperty().divide(2)));
            label.layoutYProperty().bind(d.getNode().layoutYProperty().subtract(15));

            ((javafx.scene.Group) d.getNode().getParent()).getChildren().add(label);
        }
    });
    }

    /* ----------------- Serie cumulativa saldo ----------------- */
    private static XYChart.Series<String, Number> buildSeries(List<Transaction> txs, Gran g) {
        Map<String, Money> map = switch (g) {
            case Giornaliero -> group(txs, t -> t.date().toString());
            case Mensile   -> group(txs, t -> YearMonth.from(t.date()).toString());
            case Annuale   -> group(txs, t -> Year.from(t.date()).toString());
        };

        Money running = Money.ZERO;

        XYChart.Series<String, Number> s = new XYChart.Series<>();
        for (var e : map.entrySet()) {
            running = running.add(e.getValue());
            s.getData().add(new XYChart.Data<>(e.getKey(), running.value()));
        }
        return s;
    }

    /* ----------------- raggruppa per chiave e somma importi con segno ----------------- */
    private static Map<String, Money> group(List<Transaction> txs, java.util.function.Function<Transaction,String> keyFn) {
        return txs.stream().collect(Collectors.groupingBy(keyFn, TreeMap::new, Collectors.reducing(Money.ZERO, TrendDialog::signed, Money::add)));
    }

    private static Money signed(Transaction t) {
        return t.type() == TxType.ENTRATA ? t.amount() : t.amount().negate();
    }

    private TrendDialog() {}
}
