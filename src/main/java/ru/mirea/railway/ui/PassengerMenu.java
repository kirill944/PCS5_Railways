package ru.mirea.railway.ui;

import ru.mirea.railway.exception.BusinessException;
import ru.mirea.railway.exception.DatabaseException;
import ru.mirea.railway.exception.EntityNotFoundException;
import ru.mirea.railway.model.Passenger;
import ru.mirea.railway.service.PassengerService;
import ru.mirea.railway.util.InputValidator;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

/**
 * Подменю работы с пассажирами.
 */
public class PassengerMenu {

    private final Scanner scanner;
    private final PassengerService service;

    public PassengerMenu(Scanner scanner, PassengerService service) {
        this.scanner = scanner;
        this.service = service;
    }

    public void show() {
        while (true) {
            printMenu();
            Integer choice = InputValidator.readInt(scanner, "Выберите действие: ");
            if (choice == null) continue;

            try {
                switch (choice) {
                    case 1 -> createPassenger();
                    case 2 -> listAll();
                    case 3 -> findById();
                    case 4 -> updatePassenger();
                    case 5 -> deletePassenger();
                    case 6 -> searchByName();
                    case 0 -> { return; }
                    default -> System.out.println("⚠ Неизвестный пункт меню");
                }
            } catch (BusinessException e) {
                System.out.println("✖ Нарушено правило: " + e.getMessage());
            } catch (EntityNotFoundException e) {
                System.out.println("⚠ " + e.getMessage());
            } catch (DatabaseException e) {
                System.out.println("✖ Ошибка БД: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("✖ Непредвиденная ошибка: " + e.getMessage());
            }
            System.out.println();
        }
    }

    private void printMenu() {
        System.out.println("""
                ============ ПАССАЖИРЫ ============
                1. Добавить пассажира
                2. Показать всех
                3. Найти по ID
                4. Редактировать
                5. Удалить
                6. Поиск по ФИО
                0. Назад
                ===================================
                """);
    }

    private void createPassenger() {
        System.out.println("── Создание пассажира ──");
        String fullName = InputValidator.readNonEmptyString(scanner, "ФИО: ");
        String passport = InputValidator.readNonEmptyString(scanner, "Номер паспорта: ");
        String email    = InputValidator.readNonEmptyString(scanner, "Email: ");
        String phone    = InputValidator.readNonEmptyString(scanner, "Телефон: ");
        LocalDate birth = InputValidator.readDateRequired(scanner, "Дата рождения");

        Passenger p = new Passenger(fullName, passport, email, phone, birth);
        Passenger saved = service.create(p);
        System.out.println("✔ Пассажир создан с ID=" + saved.getId());
    }

    private void listAll() {
        List<Passenger> list = service.findAll();
        TablePrinter.printPassengers(list);
    }

    private void findById() {
        Long id = InputValidator.readLong(scanner, "ID пассажира: ");
        if (id == null) return;
        Passenger p = service.findById(id);
        TablePrinter.printPassengers(List.of(p));
    }

    private void updatePassenger() {
        Long id = InputValidator.readLong(scanner, "ID пассажира для редактирования: ");
        if (id == null) return;
        Passenger existing = service.findById(id);

        System.out.println("Текущее ФИО: " + existing.getFullName());
        String fullName = InputValidator.readNonEmptyString(scanner, "Новое ФИО: ");
        String passport = InputValidator.readNonEmptyString(scanner, "Новый паспорт: ");
        String email    = InputValidator.readNonEmptyString(scanner, "Новый email: ");
        String phone    = InputValidator.readNonEmptyString(scanner, "Новый телефон: ");
        LocalDate birth = InputValidator.readDateRequired(scanner, "Новая дата рождения");

        existing.setFullName(fullName);
        existing.setPassportNumber(passport);
        existing.setEmail(email);
        existing.setPhone(phone);
        existing.setBirthDate(birth);

        service.update(existing);
        System.out.println("✔ Пассажир обновлён");
    }

    private void deletePassenger() {
        Long id = InputValidator.readLong(scanner, "ID пассажира для удаления: ");
        if (id == null) return;
        service.findById(id); // бросит EntityNotFoundException, если нет

        if (!InputValidator.confirm(scanner,
                "Удалить пассажира и все его брони?")) {
            System.out.println("Отменено");
            return;
        }
        service.delete(id);
        System.out.println("✔ Пассажир удалён");
    }

    private void searchByName() {
        String fragment = InputValidator.readNonEmptyString(scanner, "Фрагмент ФИО: ");
        List<Passenger> found = service.searchByFullName(fragment);
        TablePrinter.printPassengers(found);
    }
}