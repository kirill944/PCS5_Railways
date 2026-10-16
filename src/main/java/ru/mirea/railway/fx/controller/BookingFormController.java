package ru.mirea.railway.fx.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import ru.mirea.railway.exception.BusinessException;
import ru.mirea.railway.exception.DatabaseException;
import ru.mirea.railway.exception.EntityNotFoundException;
import ru.mirea.railway.exception.ValidationException;
import ru.mirea.railway.fx.util.AlertUtil;
import ru.mirea.railway.fx.util.FormValidation;
import ru.mirea.railway.model.Booking;
import ru.mirea.railway.model.BookingStatus;
import ru.mirea.railway.model.Passenger;
import ru.mirea.railway.service.BookingService;
import ru.mirea.railway.service.PassengerService;
import ru.mirea.railway.validation.FieldError;
import ru.mirea.railway.validation.ValidationErrors;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static ru.mirea.railway.validation.BookingValidator.*;

/**
 * Контроллер формы добавления / редактирования брони.
 *
 * Сначала разбираются введённые строки (числа, время, дата), затем
 * BookingValidator проверяет правила варианта, и только после этого
 * BookingService проверяет то, что требует БД: существование пассажира,
 * занятость места, допустимость смены статуса.
 */
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

    private final BookingService bookingService = new BookingService();
    private final PassengerService passengerService = new PassengerService();

    /** Название поля (как в сообщениях об ошибках) → элемент формы. */
    private final Map<String, Control> fields = new LinkedHashMap<>();

    private Stage dialogStage;
    private Booking existing;
    private Booking result;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        passengerBox.setItems(FXCollections.observableArrayList(
                passengerService.findAll()));
        statusBox.setItems(FXCollections.observableArrayList(BookingStatus.values()));

        fields.put(PASSENGER, passengerBox);
        fields.put(TRAIN,     trainField);
        fields.put(FROM,      fromField);
        fields.put(TO,        toField);
        fields.put(DATE,      dateField);
        fields.put(TIME,      timeField);
        fields.put(WAGON,     wagonField);
        fields.put(SEAT,      seatField);
        fields.put(PRICE,     priceField);
        fields.put(STATUS,    statusBox);
        FormValidation.clearOnEdit(fields);
    }

    public void setDialogStage(Stage stage) { this.dialogStage = stage; }

    public void setExisting(Booking existing) {
        this.existing = existing;
        if (existing == null) {
            // Новая бронь всегда создаётся в статусе «Создана»
            statusBox.setValue(BookingStatus.CREATED);
            statusBox.setDisable(true);
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
        ValidationErrors errors = new ValidationErrors();

        Passenger passenger = passengerBox.getValue();
        if (passenger == null && passengerBox.getItems().isEmpty()) {
            errors.add(PASSENGER, "в базе нет ни одного пассажира — сначала добавьте пассажира "
                    + "на вкладке «Пассажиры».");
        }

        LocalDate date  = FormValidation.readDate(dateField, DATE, errors);
        LocalTime time  = parseTime(FormValidation.text(timeField), errors);
        int wagon       = parseInt(FormValidation.text(wagonField), WAGON, MAX_WAGON, errors);
        int seat        = parseInt(FormValidation.text(seatField), SEAT, MAX_SEAT, errors);
        BigDecimal price = parsePrice(FormValidation.text(priceField), errors);

        Booking candidate = new Booking(
                existing == null ? null : existing.getId(),
                passenger == null ? null : passenger.getId(),
                FormValidation.text(trainField),
                FormValidation.text(fromField),
                FormValidation.text(toField),
                date, time, wagon, seat, price,
                statusBox.getValue(),
                existing == null ? null : existing.getCreatedAt());

        // Окно дат проверяем для новой брони или если отправление изменили
        boolean departureChanged = existing == null
                || !existing.getDepartureDate().equals(date)
                || !existing.getDepartureTime().equals(time);
        validate(candidate, departureChanged, errors);
        if (!errors.isEmpty()) {
            showErrors(errors.list());
            return;
        }

        try {
            if (existing == null) {
                result = bookingService.create(candidate);
            } else {
                bookingService.update(candidate);
                result = candidate;
            }
            dialogStage.close();
        } catch (ValidationException e) {
            // Ошибки, найденные с помощью БД: место занято, пассажир удалён, статус
            showErrors(e.getErrors());
        } catch (EntityNotFoundException e) {
            AlertUtil.error(dialogStage, "Запись не найдена",
                    e.getMessage() + ". Возможно, бронь была удалена — закройте форму и обновите список.");
        } catch (BusinessException e) {
            AlertUtil.error(dialogStage, "Нарушено правило", e.getMessage());
        } catch (DatabaseException e) {
            AlertUtil.error(dialogStage, "Ошибка БД", e.getMessage());
        }
    }

    @FXML
    private void onCancel() { dialogStage.close(); }

    // ─────────────────── разбор введённых строк ───────────────────

    private static LocalTime parseTime(String raw, ValidationErrors errors) {
        if (raw.isEmpty()) {
            return null; // «обязательное поле» сообщит BookingValidator
        }
        try {
            return LocalTime.parse(raw, TIME_FMT);
        } catch (DateTimeParseException e) {
            errors.add(TIME, "«" + raw + "» — неверное время. Введите в формате ЧЧ:ММ "
                    + "(часы 00–23, минуты 00–59), например 08:30.");
            return null;
        }
    }

    /** Возвращает 0, если число не введено или не разобрано (ошибка уже добавлена). */
    private static int parseInt(String raw, String field, int max, ValidationErrors errors) {
        if (raw.isEmpty()) {
            errors.add(field, "обязательное поле — введите целое число от 1 до " + max + ".");
            return 0;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            errors.add(field, "«" + raw + "» — не целое число. Введите число от 1 до " + max + ".");
            return 0;
        }
    }

    private static BigDecimal parsePrice(String raw, ValidationErrors errors) {
        if (raw.isEmpty()) {
            return null; // «обязательное поле» сообщит BookingValidator
        }
        try {
            return new BigDecimal(raw.replace(',', '.').replace(" ", ""));
        } catch (NumberFormatException e) {
            errors.add(PRICE, "«" + raw + "» — не число. Введите сумму в рублях, "
                    + "например 2500 или 2500,50.");
            return null;
        }
    }

    private void showErrors(List<FieldError> errors) {
        FormValidation.highlight(fields, errors);
        AlertUtil.validationErrors(dialogStage, errors);
    }
}
