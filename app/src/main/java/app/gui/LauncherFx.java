package app.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;

public final class LauncherFx extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        /* carica FXML */
        Parent root = FXMLLoader.load(getClass().getResource("/MainView.fxml"));

        /* scena + CSS custom */
        Scene scene = new Scene(root, 900, 550);
        
        new JMetro(Style.DARK).setScene(scene);

        scene.getStylesheets().add(
            getClass().getResource("/style.css").toExternalForm()
        );

        /* window */
        stage.setTitle("MoneyMinder");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
