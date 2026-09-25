package ru.mirea.railway;

import ru.mirea.railway.ui.ConsoleMenu;

/**
 * Точка входа в приложение.
 */
public class Main {

    public static void main(String[] args) {
        try {
            new ConsoleMenu().run();
        } catch (Exception e) {
            System.err.println("Критическая ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}