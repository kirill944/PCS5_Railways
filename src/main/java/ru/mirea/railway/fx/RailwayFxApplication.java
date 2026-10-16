package ru.mirea.railway.fx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.mirea.railway.util.DatabaseManager;

public class RailwayFxApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        if (!DatabaseManager.getInstance().isAvailable()) {
            throw new IllegalStateException(
                    "База данных недоступна. Проверьте db.properties и PostgreSQL.");
        }

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/main.fxml"));
        Scene scene = new Scene(loader.load(), 1200, 720);
        scene.getStylesheets().add(
                getClass().getResource("/css/app.css").toExternalForm());

        stage.setTitle("Бронирование Ж/Д билетов");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(600);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}