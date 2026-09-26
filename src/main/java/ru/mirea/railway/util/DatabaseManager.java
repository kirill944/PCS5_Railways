package ru.mirea.railway.util;

import static ru.mirea.railway.util.Gradient.*;

import ru.mirea.railway.exception.DatabaseException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Менеджер подключения к базе данных PostgreSQL.
 *
 * Реализован как Singleton — единая точка получения Connection
 * для всего приложения.
 *
 * Параметры подключения читаются из файла db.properties,
 * который лежит в src/main/resources/.
 *
 * Пример db.properties:
 *   db.url=jdbc:postgresql://localhost:5432/railway_booking
 *   db.user=postgres
 *   db.password=postgres
 *   db.driver=org.postgresql.Driver
 */
public final class DatabaseManager {

    private static final String CONFIG_FILE = "db.properties";

    private static DatabaseManager instance;

    private final String url;
    private final String user;
    private final String password;

    // Приватный конструктор — Singleton
    private DatabaseManager() {
        Properties props = loadProperties();
        this.url      = props.getProperty("db.url");
        this.user     = props.getProperty("db.user");
        this.password = props.getProperty("db.password");
        String driver = props.getProperty("db.driver", "org.postgresql.Driver");

        if (url == null || user == null || password == null) {
            throw new DatabaseException(between(
                    "Не заданы параметры подключения в " + CONFIG_FILE, PINK, VIOLET));
        }

        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new DatabaseException(between(
                    "JDBC-драйвер не найден: " + driver, PINK, VIOLET), null);
        }
    }

    /** Ленивая инициализация Singleton (потокобезопасно). */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Возвращает новое соединение с БД.
     * Вызывающий код обязан закрыть его (try-with-resources).
     */
    public Connection getConnection() {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new DatabaseException(between(
                    "Не удалось подключиться к БД: " + e.getMessage(), PINK, VIOLET), e);
        }
    }

    /** Удобный статический метод-обёртка. */
    public static Connection getConnectionStatic() {
        return getInstance().getConnection();
    }

    /** Проверка доступности БД. */
    public boolean isAvailable() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException | DatabaseException e) {
            return false;
        }
    }

    // --- Внутренние методы ---

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = getClass()
                .getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {

            if (in == null) {
                throw new DatabaseException(between(
                        "Файл " + CONFIG_FILE + " не найден в classpath", PINK, VIOLET));
            }
            props.load(in);
            return props;
        } catch (IOException e) {
            throw new DatabaseException(between(
                    "Ошибка чтения " + CONFIG_FILE, PINK, VIOLET), null);
        }
    }
}