package ru.mirea.railway.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ru.mirea.railway.exception.BusinessException;
import ru.mirea.railway.exception.DatabaseException;
import ru.mirea.railway.exception.EntityNotFoundException;
import ru.mirea.railway.fx.util.AlertUtil;
import ru.mirea.railway.model.Passenger;
import ru.mirea.railway.service.PassengerService;

import java.time.LocalDate;

/**
 * Контроллер формы добавления / редактирования пассажира.
 *
 * Используется как модальное диалоговое окно из MainController.
 * Поля ввода описаны в passenger-form.fxml, бизнес-валидация —
 * в PassengerService (там же — уникальность email и паспорта).
 */
public class PassengerFormController {

    @FXML private TextField  fullNameField;
    @FXML private TextField  passportField;
    @FXML private TextField  emailField;
    @FXML private TextField  phoneField;
    @FXML private DatePicker birthDateField;
    @FXML private Label      errorLabel;

    private final PassengerService passengerService = new PassengerService();

    private Stage dialogStage;
    private Passenger existing;
    private Passenger result;

    /** Устанавливает окно диалога — вызывается MainController'ом. */
    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    /**
     * Передаёт существующего пассажира для редактирования.
     * При existing == null форма работает в режиме создания.
     */
    public void setExisting(Passenger existing) {
        this.existing = existing;
        if (existing == null) {
            return;
        }
        fullNameField.setText(existing.getFullName());
        passportField.setText(existing.getPassportNumber());
        emailField.setText(existing.getEmail());
        phoneField.setText(existing.getPhone());
        birthDateField.setValue(existing.getBirthDate());
    }

    /** Результат сохранения — читается MainController'ом после showAndWait(). */
    public Passenger getResult() {
        return result;
    }

    // ─────────────────── сохранение ───────────────────

    @FXML
    private void onSave() {
        errorLabel.setText("");

        String fullName = fullNameField.getText() == null
                ? "" : fullNameField.getText().trim();
        String passport = passportField.getText() == null
                ? "" : passportField.getText().trim();
        String email = emailField.getText() == null
                ? "" : emailField.getText().trim();
        String phone = phoneField.getText() == null
                ? "" : phoneField.getText().trim();
        LocalDate birthDate = birthDateField.getValue();

        // Простые проверки на пустоту — до обращения к сервису,
        // чтобы сразу подсказать пользователю, какое поле не заполнено.
        if (fullName.isEmpty())  { fail("Введите ФИО"); return; }
        if (passport.isEmpty())  { fail("Введите паспорт"); return; }
        if (email.isEmpty())     { fail("Введите email"); return; }
        if (phone.isEmpty())     { fail("Введите телефон"); return; }
        if (birthDate == null)   { fail("Выберите дату рождения"); return; }

        try {
            if (existing == null) {
                Passenger p = new Passenger(fullName, passport, email, phone, birthDate);
                result = passengerService.create(p);
            } else {
                existing.setFullName(fullName);
                existing.setPassportNumber(passport);
                existing.setEmail(email);
                existing.setPhone(phone);
                existing.setBirthDate(birthDate);
                passengerService.update(existing);
                result = existing;
            }
            dialogStage.close();
        } catch (BusinessException e) {
            // Нарушено бизнес-правило: формат поля или уникальность
            fail(e.getMessage());
        } catch (EntityNotFoundException e) {
            fail(e.getMessage());
        } catch (DatabaseException e) {
            AlertUtil.error("Ошибка БД", e.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        dialogStage.close();
    }

    private void fail(String message) {
        errorLabel.setText(message);
    }
}