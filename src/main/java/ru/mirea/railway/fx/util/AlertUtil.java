package ru.mirea.railway.fx.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public final class AlertUtil {
    private AlertUtil() {}

    public static void info(String title, String message) {
        alert(Alert.AlertType.INFORMATION, title, message);
    }
    public static void warn(String title, String message) {
        alert(Alert.AlertType.WARNING, title, message);
    }
    public static void error(String title, String message) {
        alert(Alert.AlertType.ERROR, title, message);
    }

    public static boolean confirm(String title, String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, message,
                ButtonType.OK, ButtonType.CANCEL);
        a.setTitle(title);
        a.setHeaderText(null);
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    private static void alert(Alert.AlertType type, String title, String message) {
        Alert a = new Alert(type, message, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }
}