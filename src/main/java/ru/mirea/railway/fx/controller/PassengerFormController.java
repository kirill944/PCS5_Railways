package ru.mirea.railway.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ru.mirea.railway.exception.BusinessException;
import ru.mirea.railway.exception.DatabaseException;
import ru.mirea.railway.exception.EntityNotFoundException;
import ru.mirea.railway.exception.ValidationException;
import ru.mirea.railway.fx.util.AlertUtil;
import ru.mirea.railway.fx.util.FormValidation;
import ru.mirea.railway.model.Passenger;
import ru.mirea.railway.service.PassengerService;
import ru.mirea.railway.validation.FieldError;
import ru.mirea.railway.validation.PassengerValidator;
import ru.mirea.railway.validation.ValidationErrors;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Контроллер формы добавления / редактирования пассажира.
 *
 * Используется как модальное диалоговое окно из MainController.
 * Поля ввода описаны в passenger-form.fxml. Формат полей проверяет
 * PassengerValidator, уникальность email и паспорта — PassengerService.
 * Ошибки показываются отдельным окном, неверные поля подсвечиваются.
 */
public class PassengerFormController {

    @FXML private TextField  fullNameField;
    @FXML private TextField  passportField;
    @FXML private TextField  emailField;
    @FXML private TextField  phoneField;
    @FXML private DatePicker birthDateField;

    private final PassengerService passengerService = new PassengerService();

    /** Название поля (как в сообщениях об ошибках) → элемент формы. */
    private final Map<String, Control> fields = new LinkedHashMap<>();

    private Stage dialogStage;
    private Passenger existing;
    private Passenger result;

    @FXML
    private void initialize() {
        fields.put(PassengerValidator.FULL_NAME,  fullNameField);
        fields.put(PassengerValidator.PASSPORT,   passportField);
        fields.put(PassengerValidator.EMAIL,      emailField);
        fields.put(PassengerValidator.PHONE,      phoneField);
        fields.put(PassengerValidator.BIRTH_DATE, birthDateField);
        FormValidation.clearOnEdit(fields);
    }

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
        ValidationErrors errors = new ValidationErrors();

        LocalDate birthDate = FormValidation.readDate(
                birthDateField, PassengerValidator.BIRTH_DATE, errors);
        Passenger candidate = new Passenger(
                existing == null ? null : existing.getId(),
                FormValidation.text(fullNameField),
                FormValidation.text(passportField),
                FormValidation.text(emailField),
                FormValidation.text(phoneField),
                birthDate);

        // Проверяем всё до обращения к БД — пользователь сразу видит все ошибки
        PassengerValidator.validate(candidate, errors);
        if (!errors.isEmpty()) {
            showErrors(errors.list());
            return;
        }

        try {
            if (existing == null) {
                result = passengerService.create(candidate);
            } else {
                passengerService.update(candidate);
                result = candidate;
            }
            dialogStage.close();
        } catch (ValidationException e) {
            // Ошибки, найденные с помощью БД: паспорт или email уже заняты
            showErrors(e.getErrors());
        } catch (EntityNotFoundException e) {
            AlertUtil.error(dialogStage, "Запись не найдена",
                    e.getMessage() + ". Возможно, пассажир был удалён — закройте форму и обновите список.");
        } catch (BusinessException e) {
            AlertUtil.error(dialogStage, "Нарушено правило", e.getMessage());
        } catch (DatabaseException e) {
            AlertUtil.error(dialogStage, "Ошибка БД", e.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        dialogStage.close();
    }

    private void showErrors(List<FieldError> errors) {
        FormValidation.highlight(fields, errors);
        AlertUtil.validationErrors(dialogStage, errors);
    }
}
