package ru.mirea.railway.fx.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import ru.mirea.railway.validation.FieldError;

import java.util.List;

public final class AlertUtil {
    private AlertUtil() {}

    private static final String ANSI_REGEX = "\u001B\\[[;\\d]*m";

    public static void info(String title, String message) {
        alert(null, Alert.AlertType.INFORMATION, title, message);
    }
    public static void warn(String title, String message) {
        alert(null, Alert.AlertType.WARNING, title, message);
    }
    public static void error(String title, String message) {
        alert(null, Alert.AlertType.ERROR, title, message);
    }
    public static void error(Window owner, String title, String message) {
        alert(owner, Alert.AlertType.ERROR, title, message);
    }

    public static boolean confirm(String title, String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, plain(message),
                ButtonType.OK, ButtonType.CANCEL);
        a.setTitle(title);
        a.setHeaderText(null);
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    public static void validationErrors(Window owner, List<FieldError> errors) {
        Alert a = new Alert(Alert.AlertType.ERROR, null, ButtonType.OK);
        if (owner != null) {
            a.initOwner(owner);
        }
        a.setTitle("Ошибка ввода данных");
        a.setHeaderText(errors.size() == 1
                ? "Поле заполнено неверно"
                : "Неверно заполнены поля: " + errors.size());

        VBox list = new VBox(8);
        for (FieldError e : errors) {
            Label line = new Label("• " + e.field() + " — " + plain(e.message()));
            line.setWrapText(true);
            list.getChildren().add(line);
        }
        Label hint = new Label("Исправьте отмеченные поля и нажмите «Сохранить» ещё раз.");
        hint.setWrapText(true);
        hint.setStyle("-fx-text-fill: #57606f; -fx-padding: 6 0 0 0;");
        list.getChildren().add(hint);

        a.getDialogPane().setContent(list);
        a.getDialogPane().setPrefWidth(520);
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        a.showAndWait();
    }

    private static void alert(Window owner, Alert.AlertType type, String title, String message) {
        Alert a = new Alert(type, plain(message), ButtonType.OK);
        if (owner != null) {
            a.initOwner(owner);
        }
        a.setTitle(title);
        a.setHeaderText(null);
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        a.showAndWait();
    }

    private static String plain(String message) {
        return message == null ? "" : message.replaceAll(ANSI_REGEX, "");
    }
}