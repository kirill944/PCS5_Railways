package ru.mirea.railway.fx.util;

import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import ru.mirea.railway.validation.FieldError;
import ru.mirea.railway.validation.ValidationErrors;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Вспомогательные методы для форм: чтение значений полей
 * и подсветка полей, заполненных неверно.
 */
public final class FormValidation {

    private static final String ERROR_CLASS = "field-error";

    private FormValidation() {}

    /** Текст поля без пробелов по краям (никогда не null). */
    public static String text(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    /**
     * Читает дату из DatePicker, включая введённую вручную.
     * Если текст не удаётся разобрать — добавляет ошибку и возвращает null.
     */
    public static LocalDate readDate(DatePicker picker, String fieldName, ValidationErrors errors) {
        String raw = picker.getEditor().getText() == null ? "" : picker.getEditor().getText().trim();
        if (raw.isEmpty()) {
            picker.setValue(null);
            return null;
        }
        try {
            LocalDate date = picker.getConverter().fromString(raw);
            picker.setValue(date);
            return date;
        } catch (RuntimeException e) {
            errors.add(fieldName, "дата «" + raw + "» введена неверно — используйте формат "
                    + "ДД.ММ.ГГГГ или выберите дату в календаре.");
            return null;
        }
    }

    /**
     * Подсвечивает поля с ошибками и ставит фокус на первое из них.
     * fields — соответствие «название поля → элемент формы».
     */
    public static void highlight(Map<String, ? extends Control> fields, List<FieldError> errors) {
        fields.values().forEach(c -> c.getStyleClass().remove(ERROR_CLASS));
        Control first = null;
        for (FieldError e : errors) {
            Control c = fields.get(e.field());
            if (c == null) continue;
            if (!c.getStyleClass().contains(ERROR_CLASS)) {
                c.getStyleClass().add(ERROR_CLASS);
            }
            if (first == null) first = c;
        }
        if (first != null) {
            first.requestFocus();
        }
    }

    /** Снимает подсветку с поля, как только пользователь начинает его исправлять. */
    public static void clearOnEdit(Map<String, ? extends Control> fields) {
        for (Control c : fields.values()) {
            Runnable clear = () -> c.getStyleClass().remove(ERROR_CLASS);
            if (c instanceof DatePicker dp) {
                dp.valueProperty().addListener((o, a, b) -> clear.run());
                dp.getEditor().textProperty().addListener((o, a, b) -> clear.run());
            } else if (c instanceof TextField tf) {
                tf.textProperty().addListener((o, a, b) -> clear.run());
            } else if (c instanceof ComboBox<?> cb) {
                cb.valueProperty().addListener((o, a, b) -> clear.run());
            }
        }
    }
}
