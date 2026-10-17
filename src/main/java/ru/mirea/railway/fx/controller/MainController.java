package ru.mirea.railway.fx.controller;

import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import ru.mirea.railway.exception.BusinessException;
import ru.mirea.railway.exception.DatabaseException;
import ru.mirea.railway.exception.EntityNotFoundException;
import ru.mirea.railway.fx.util.AlertUtil;
import ru.mirea.railway.fx.util.CsvExporter;
import ru.mirea.railway.model.Booking;
import ru.mirea.railway.model.BookingStatus;
import ru.mirea.railway.model.Passenger;
import ru.mirea.railway.service.BookingService;
import ru.mirea.railway.service.PassengerService;
import ru.mirea.railway.service.StatisticsService;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class MainController implements Initializable {

    @FXML private StackPane contentPane;
    @FXML private TextField searchField;
    @FXML private VBox bookingsPane;
    @FXML private VBox passengersPane;

    @FXML private TableView<Booking> bookingTable;
    @FXML private TableColumn<Booking, Long>    colId;
    @FXML private TableColumn<Booking, String>  colPassenger;
    @FXML private TableColumn<Booking, String>  colTrain;
    @FXML private TableColumn<Booking, String>  colFrom;
    @FXML private TableColumn<Booking, String>  colTo;
    @FXML private TableColumn<Booking, String>  colDate;
    @FXML private TableColumn<Booking, String>  colTime;
    @FXML private TableColumn<Booking, Integer> colWagon;
    @FXML private TableColumn<Booking, Integer> colSeat;
    @FXML private TableColumn<Booking, String>  colPrice;
    @FXML private TableColumn<Booking, String>  colStatus;

    @FXML private TableView<Passenger> passengerTable;
    @FXML private TableColumn<Passenger, Long>   colPId;
    @FXML private TableColumn<Passenger, String> colPName;
    @FXML private TableColumn<Passenger, String> colPPassport;
    @FXML private TableColumn<Passenger, String> colPEmail;
    @FXML private TableColumn<Passenger, String> colPPhone;
    @FXML private TableColumn<Passenger, String> colPBirth;

    @FXML private ComboBox<BookingStatus> statusFilter;
    @FXML private DatePicker dateFrom;
    @FXML private DatePicker dateTo;
    @FXML private Label statusLabel;
    @FXML private Label warningLabel;

    private final BookingService bookingService = new BookingService();
    private final PassengerService passengerService = new PassengerService();
    private final StatisticsService statisticsService = new StatisticsService();

    private Map<Long, String> passengerNames = new HashMap<>();

    private final ObservableList<Booking>   bookings   = FXCollections.observableArrayList();
    private final ObservableList<Passenger> passengers = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupBookingColumns();
        setupPassengerColumns();
        setupFilters();

        bookingTable.setItems(bookings);
        bookingTable.setPlaceholder(new Label("Нет данных"));

        passengerTable.setItems(passengers);
        passengerTable.setPlaceholder(new Label("Нет данных"));

        bookingTable.setRowFactory(tv -> {
            TableRow<Booking> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty()
                        && e.getButton() == MouseButton.PRIMARY
                        && e.getClickCount() == 2) onEdit();
            });
            return row;
        });
        passengerTable.setRowFactory(tv -> {
            TableRow<Passenger> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty()
                        && e.getButton() == MouseButton.PRIMARY
                        && e.getClickCount() == 2) onEditPassenger();
            });
            return row;
        });

        showBookings();

        refreshBookings();
        refreshPassengers();
    }

    @FXML
    private void showBookings() {
        bookingsPane.setVisible(true);
        bookingsPane.setManaged(true);
        passengersPane.setVisible(false);
        passengersPane.setManaged(false);
        statusLabel.setText("Раздел: Бронирования");
        clearWarning();
    }

    @FXML
    private void showPassengers() {
        passengersPane.setVisible(true);
        passengersPane.setManaged(true);
        bookingsPane.setVisible(false);
        bookingsPane.setManaged(false);
        statusLabel.setText("Раздел: Пассажиры");
        clearWarning();
    }

    private void setupBookingColumns() {
        colId.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getId()));
        colPassenger.setCellValueFactory(c -> new SimpleStringProperty(
                passengerNames.getOrDefault(c.getValue().getPassengerId(),
                        "ID=" + c.getValue().getPassengerId())));
        colTrain.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getTrainNumber()));
        colFrom.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getRouteFrom()));
        colTo.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getRouteTo()));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDepartureDate() == null ? "" :
                        c.getValue().getDepartureDate().toString()));
        colTime.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDepartureTime() == null ? "" :
                        c.getValue().getDepartureTime().toString()));
        colWagon.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getWagonNumber()));
        colSeat.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getSeatNumber()));
        colPrice.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPrice() == null ? "" :
                        c.getValue().getPrice().toPlainString()));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStatus() == null ? "" :
                        c.getValue().getStatus().getDisplayName()));
    }

    private void setupPassengerColumns() {
        colPId.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getId()));
        colPName.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getFullName()));
        colPPassport.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getPassportNumber()));
        colPEmail.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getEmail()));
        colPPhone.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getPhone()));
        colPBirth.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getBirthDate() == null ? "" :
                        c.getValue().getBirthDate().toString()));
    }

    private void setupFilters() {
        statusFilter.setItems(FXCollections.observableArrayList(BookingStatus.values()));
    }

    private void reloadPassengerNames() {
        passengerNames = passengerService.findAll().stream()
                .collect(Collectors.toMap(Passenger::getId, Passenger::getFullName));
    }

    private void refreshBookings() {
        try {
            reloadPassengerNames();
            bookings.setAll(bookingService.findAll());
        } catch (DatabaseException e) {
            AlertUtil.error("Ошибка БД", e.getMessage());
        }
    }

    private void refreshPassengers() {
        try {
            passengers.setAll(passengerService.findAll());
        } catch (DatabaseException e) {
            AlertUtil.error("Ошибка БД", e.getMessage());
        }
    }

    @FXML
    private void onAdd() {
        Booking created = openBookingForm(null);
        if (created != null) refreshBookings();
    }

    @FXML
    private void onEdit() {
        Booking selected = bookingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warn("Не выбрана запись", "Выберите строку для редактирования.");
            return;
        }
        Booking updated = openBookingForm(selected);
        if (updated != null) refreshBookings();
    }

    @FXML
    private void onDelete() {
        Booking selected = bookingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warn("Не выбрана запись", "Выберите строку для удаления.");
            return;
        }
        if (!AlertUtil.confirm("Удаление",
                "Удалить бронь ID=" + selected.getId() + "?")) return;
        try {
            bookingService.delete(selected.getId());
            refreshBookings();
        } catch (EntityNotFoundException e) {
            AlertUtil.warn("Не найдено", e.getMessage());
        } catch (BusinessException e) {
            AlertUtil.error("Нарушено правило", e.getMessage());
        } catch (DatabaseException e) {
            AlertUtil.error("Ошибка БД", e.getMessage());
        }
    }

    @FXML
    private void onRefresh() {
        statusFilter.setValue(null);
        dateFrom.setValue(null);
        dateTo.setValue(null);
        clearWarning();
        refreshBookings();
        statusLabel.setText("Бронирования обновлены. Записей: " + bookings.size());
    }

    private Booking openBookingForm(Booking existing) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/booking-form.fxml"));
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(existing == null ? "Новая бронь" : "Редактирование брони");
            dialog.setScene(new Scene(loader.load()));

            BookingFormController c = loader.getController();
            c.setDialogStage(dialog);
            c.setExisting(existing);

            dialog.showAndWait();
            return c.getResult();
        } catch (Exception e) {
            AlertUtil.error("Ошибка", "Не удалось открыть форму: " + e.getMessage());
            return null;
        }
    }

    @FXML
    private void onAddPassenger() {
        Passenger created = openPassengerForm(null);
        if (created != null) {
            refreshPassengers();
            refreshBookings();
        }
    }

    @FXML
    private void onEditPassenger() {
        Passenger selected = passengerTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warn("Не выбрана запись", "Выберите пассажира.");
            return;
        }
        Passenger updated = openPassengerForm(selected);
        if (updated != null) {
            refreshPassengers();
            refreshBookings();
        }
    }

    @FXML
    private void onDeletePassenger() {
        Passenger selected = passengerTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warn("Не выбрана запись", "Выберите пассажира.");
            return;
        }
        if (!AlertUtil.confirm("Удаление",
                "Удалить пассажира «" + selected.getFullName() + "» и все его брони?")) return;
        try {
            passengerService.delete(selected.getId());
            refreshPassengers();
            refreshBookings();
        } catch (EntityNotFoundException e) {
            AlertUtil.warn("Не найдено", e.getMessage());
        } catch (BusinessException e) {
            AlertUtil.error("Нарушено правило", e.getMessage());
        } catch (DatabaseException e) {
            AlertUtil.error("Ошибка БД", e.getMessage());
        }
    }

    @FXML
    private void onRefreshPassengers() {
        refreshPassengers();
        statusLabel.setText("Пассажиры обновлены. Записей: " + passengers.size());
    }

    private Passenger openPassengerForm(Passenger existing) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/passenger-form.fxml"));
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(existing == null ? "Новый пассажир" : "Редактирование пассажира");
            dialog.setScene(new Scene(loader.load()));

            PassengerFormController c = loader.getController();
            c.setDialogStage(dialog);
            c.setExisting(existing);

            dialog.showAndWait();
            return c.getResult();
        } catch (Exception e) {
            AlertUtil.error("Ошибка", "Не удалось открыть форму: " + e.getMessage());
            return null;
        }
    }

    @FXML
    private void onSearch() {
        String q = searchField.getText();
        if (q == null || q.isBlank()) {
            refreshBookings();
            statusLabel.setText("Пустой запрос — показаны все записи.");
            clearWarning();
            return;
        }
        try {
            Set<Booking> result = new LinkedHashSet<>();
            result.addAll(bookingService.searchByTrainNumber(q));
            result.addAll(bookingService.searchByRoute(q, q));
            result.addAll(bookingService.searchByPassengerName(q));
            bookings.setAll(result);
            statusLabel.setText("Найдено: " + result.size());
            clearWarning();
        } catch (BusinessException e) {
            AlertUtil.warn("Поиск", e.getMessage());
        } catch (DatabaseException e) {
            AlertUtil.error("Ошибка БД", e.getMessage());
        }
    }

    @FXML
    private void onResetSearch() {
        searchField.clear();
        refreshBookings();
        statusLabel.setText("Поиск сброшен.");
        clearWarning();
    }

    @FXML
    private void onFilter() {
        BookingStatus status = statusFilter.getValue();
        LocalDate from = dateFrom.getValue();
        LocalDate to   = dateTo.getValue();

        if (from == null && to == null) {
            showWarning("Выберите Даты от и до");
            return;
        }
        if (from == null) {
            showWarning("Выберите Дату от");
            return;
        }
        if (to == null) {
            showWarning("Выберите Дату до");
            return;
        }
        if (from.isAfter(to)) {
            showWarning("Дата от не может быть позже Даты до");
            return;
        }

        try {
            List<Booking> result = bookingService.findAll();

            if (status != null) {
                result = result.stream()
                        .filter(b -> b.getStatus() == status)
                        .collect(Collectors.toList());
            }
            result = result.stream()
                    .filter(b -> b.getDepartureDate() != null
                            && !b.getDepartureDate().isBefore(from)
                            && !b.getDepartureDate().isAfter(to))
                    .collect(Collectors.toList());

            bookings.setAll(result);
            clearWarning();
            statusLabel.setText("После фильтра: " + result.size()
                    + " (" + from + " — " + to
                    + (status != null ? ", " + status.getDisplayName() : "") + ")");
        } catch (DatabaseException e) {
            AlertUtil.error("Ошибка БД", e.getMessage());
        }
    }

    @FXML
    private void onResetFilter() {
        statusFilter.setValue(null);
        dateFrom.setValue(null);
        dateTo.setValue(null);
        clearWarning();
        refreshBookings();
        statusLabel.setText("Фильтр сброшен.");
    }

    @FXML private void onSortDateAsc()  { sortBookings(Comparator.comparing(Booking::getDepartureDate,
            Comparator.nullsLast(Comparator.naturalOrder()))); }
    @FXML private void onSortDateDesc() { sortBookings(Comparator.comparing(Booking::getDepartureDate,
            Comparator.nullsLast(Comparator.naturalOrder())).reversed()); }
    @FXML private void onSortPriceAsc() { sortBookings(Comparator.comparing(Booking::getPrice,
            Comparator.nullsLast(Comparator.naturalOrder()))); }
    @FXML private void onSortPriceDesc(){ sortBookings(Comparator.comparing(Booking::getPrice,
            Comparator.nullsLast(Comparator.naturalOrder())).reversed()); }

    private void sortBookings(Comparator<Booking> cmp) {
        List<Booking> sorted = new ArrayList<>(bookings);
        sorted.sort(cmp);
        bookings.setAll(sorted);
    }

    @FXML
    private void onStatistics() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/statistics.fxml"));
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Статистика");
            dialog.setScene(new Scene(loader.load()));

            StatisticsController c = loader.getController();
            c.load(statisticsService.collect());

            dialog.showAndWait();
        } catch (Exception e) {
            AlertUtil.error("Ошибка", "Не удалось открыть статистику: " + e.getMessage());
        }
    }

    @FXML
    private void onExport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить CSV");
        chooser.setInitialFileName("bookings.csv");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV", "*.csv"));

        File file = chooser.showSaveDialog(bookingTable.getScene().getWindow());
        if (file == null) return;

        try {
            CsvExporter.exportBookings(bookings, passengerNames, file.toPath());
            AlertUtil.info("Экспорт", "Сохранено: " + file.getAbsolutePath());
        } catch (Exception e) {
            AlertUtil.error("Ошибка экспорта", e.getMessage());
        }
    }

    private void showWarning(String message) {
        warningLabel.setText(message);
        warningLabel.setVisible(true);
        warningLabel.setManaged(true);

        PauseTransition pause = new PauseTransition(Duration.seconds(4));
        pause.setOnFinished(e -> clearWarning());
        pause.play();
    }

    private void clearWarning() {
        warningLabel.setVisible(false);
        warningLabel.setManaged(false);
        warningLabel.setText("");
    }
}