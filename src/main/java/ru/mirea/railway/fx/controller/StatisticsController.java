package ru.mirea.railway.fx.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import ru.mirea.railway.service.StatisticsService;

public class StatisticsController {

    @FXML private Label lblPassengers;
    @FXML private Label lblBookings;
    @FXML private Label lblActive;
    @FXML private Label lblCompleted;
    @FXML private Label lblCancelled;
    @FXML private Label lblAvgPrice;
    @FXML private ListView<String> byTrainList;

    public void load(StatisticsService.Statistics s) {
        lblPassengers.setText(String.valueOf(s.totalPassengers()));
        lblBookings.setText(String.valueOf(s.totalBookings()));
        lblActive.setText(String.valueOf(s.activeBookings()));
        lblCompleted.setText(String.valueOf(s.completedBookings()));
        lblCancelled.setText(String.valueOf(s.cancelledBookings()));
        lblAvgPrice.setText(s.averagePrice() + " ₽");

        byTrainList.setItems(FXCollections.observableArrayList(
                s.bookingsByTrain().entrySet().stream()
                        .map(e -> e.getKey() + " → " + e.getValue())
                        .toList()));
    }

    @FXML
    private void onClose() {
        ((Stage) byTrainList.getScene().getWindow()).close();
    }
}