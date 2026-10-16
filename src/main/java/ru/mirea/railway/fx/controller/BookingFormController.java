package ru.mirea.railway.fx.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import ru.mirea.railway.exception.BusinessException;
import ru.mirea.railway.exception.DatabaseException;
import ru.mirea.railway.exception.EntityNotFoundException;
import ru.mirea.railway.fx.util.AlertUtil;
import ru.mirea.railway.model.Booking;
import ru.mirea.railway.model.BookingStatus;
import ru.mirea.railway.model.Passenger;
import ru.mirea.railway.service.BookingService;
import ru.mirea.railway.service.PassengerService;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class BookingFormController implements Initializable {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    @FXML private ComboBox<Passenger> passengerBox;
    @FXML private TextField trainField;
    @FXML private TextField fromField;
    @FXML private TextField toField;
    @FXML private DatePicker dateField;
    @FXML private TextField timeField;
    @FXML private TextField wagonField;
    @FXML private TextField seatField;
    @FXML private TextField priceField;
    @FXML private ComboBox<BookingStatus> statusBox;
    @FXML private Label errorLabel;

    private final BookingService bookingService = new BookingService();
    private final PassengerService passengerService = new PassengerService();

    private Stage dialogStage;
    private Booking existing;
    private Booking result;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        passengerBox.setItems(FXCollections.observableArrayList(
                passengerService.findAll()));
        statusBox.setItems(FXCollections.observableArrayList(BookingStatus.values()));
    }

    public void setDialogStage(Stage stage) { this.dialogStage = stage; }

    public void setExisting(Booking existing) {
        this.existing = existing;
        if (existing == null) {
            statusBox.setValue(BookingStatus.CREATED);
            return;
        }
        passengerBox.getItems().stream()
                .filter(p -> p.getId().equals(existing.getPassengerId()))
                .findFirst()
                .ifPresent(passengerBox::setValue);
        trainField.setText(existing.getTrainNumber());
        fromField.setText(existing.getRouteFrom());
        toField.setText(existing.getRouteTo());
        dateField.setValue(existing.getDepartureDate());
        timeField.setText(existing.getDepartureTime() == null ? "" :
                existing.getDepartureTime().format(TIME_FMT));
        wagonField.setText(String.valueOf(existing.getWagonNumber()));
        seatField.setText(String.valueOf(existing.getSeatNumber()));
        priceField.setText(existing.getPrice() == null ? "" :
                existing.getPrice().toPlainString());
        statusBox.setValue(existing.getStatus());
    }

    public Booking getResult() { return result; }

    @FXML
    private void onSave() {
        errorLabel.setText("");

        Passenger passenger = passengerBox.getValue();
        if (passenger == null) { fail("Выберите пассажира"); return; }

        String train = trainField.getText().trim();
        if (train.isEmpty()) { fail("Введите номер поезда"); return; }

        String from = fromField.getText().trim();
        String to   = toField.getText().trim();
        if (from.isEmpty() || to.isEmpty()) { fail("Заполните станции"); return; }

        LocalDate date = dateField.getValue();
        if (date == null) { fail("Выберите дату отправления"); return; }

        LocalTime time;
        try {
            time = LocalTime.parse(timeField.getText().trim(), TIME_FMT);
        } catch (Exception e) {
            fail("Время в формате HH:mm"); return;
        }

        int wagon, seat;
        try { wagon = Integer.parseInt(wagonField.getText().trim()); }
        catch (NumberFormatException e) { fail("Вагон — целое число"); return; }
        try { seat = Integer.parseInt(seatField.getText().trim()); }
        catch (NumberFormatException e) { fail("Место — целое число"); return; }

        BigDecimal price;
        try { price = new BigDecimal(priceField.getText().trim().replace(',', '.')); }
        catch (NumberFormatException e) { fail("Цена — число"); return; }

        BookingStatus status = statusBox.getValue();
        if (status == null) { fail("Выберите статус"); return; }

        try {
            if (existing == null) {
                Booking b = new Booking(passenger.getId(), train, from, to,
                        date, time, wagon, seat, price, status);
                result = bookingService.create(b);
            } else {
                existing.setPassengerId(passenger.getId());
                existing.setTrainNumber(train);
                existing.setRouteFrom(from);
                existing.setRouteTo(to);
                existing.setDepartureDate(date);
                existing.setDepartureTime(time);
                existing.setWagonNumber(wagon);
                existing.setSeatNumber(seat);
                existing.setPrice(price);
                existing.setStatus(status);
                bookingService.update(existing);
                result = existing;
            }
            dialogStage.close();
        } catch (BusinessException e) {
            fail(e.getMessage());
        } catch (EntityNotFoundException e) {
            fail(e.getMessage());
        } catch (DatabaseException e) {
            AlertUtil.error("Ошибка БД", e.getMessage());
        }
    }

    private void fail(String msg) { errorLabel.setText(msg); }

    @FXML
    private void onCancel() { dialogStage.close(); }
}