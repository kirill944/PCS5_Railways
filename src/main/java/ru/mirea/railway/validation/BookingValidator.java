package ru.mirea.railway.validation;

import ru.mirea.railway.model.Booking;
import ru.mirea.railway.util.InputValidator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Проверка данных бронирования перед сохранением.
 *
 * Правила варианта:
 *   - номер поезда: 3 цифры + заглавная русская буква (без Ь, Ъ, Ё, Ы);
 *   - станции: 2–100 символов, не совпадают;
 *   - дата отправления: не в прошлом и не позже года вперёд,
 *     при отправлении сегодня — время ещё не прошло;
 *   - вагон: 1–20, место: 1–50;
 *   - цена: 1000–100 000 ₽, не более 2 знаков после запятой.
 *
 * Существование пассажира, занятость места и допустимость смены
 * статуса проверяются в BookingService — для этого нужна БД.
 */
public final class BookingValidator {

    public static final String PASSENGER = "Пассажир";
    public static final String TRAIN     = "Номер поезда";
    public static final String FROM      = "Откуда";
    public static final String TO        = "Куда";
    public static final String DATE      = "Дата отправления";
    public static final String TIME      = "Время";
    public static final String WAGON     = "Вагон";
    public static final String SEAT      = "Место";
    public static final String PRICE     = "Цена";
    public static final String STATUS    = "Статус";

    public static final int MAX_WAGON      = 20;
    public static final int MAX_SEAT       = 50;
    public static final int STATION_MIN    = 2;
    public static final int STATION_MAX    = 100;
    public static final int MAX_DAYS_AHEAD = 365;

    public static final BigDecimal MIN_PRICE = new BigDecimal("1000");
    public static final BigDecimal MAX_PRICE = new BigDecimal("100000");

    private static final String STATION_REGEX = "^[А-Яа-яЁёA-Za-z0-9 .()-]+$";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private BookingValidator() {}

    /** Проверяет бронь и бросает ValidationException со всеми ошибками. */
    public static void validate(Booking b, boolean checkDepartureWindow) {
        ValidationErrors errors = new ValidationErrors();
        validate(b, checkDepartureWindow, errors);
        errors.throwIfAny();
    }

    /**
     * Проверяет бронь, добавляя найденные ошибки в errors.
     *
     * @param checkDepartureWindow проверять ли, что отправление в будущем
     *                             и не дальше года. При редактировании брони
     *                             без изменения даты/времени проверка
     *                             не нужна — иначе нельзя поправить
     *                             уже состоявшуюся поездку.
     */
    public static void validate(Booking b, boolean checkDepartureWindow, ValidationErrors errors) {
        if (b.getPassengerId() == null) {
            errors.add(PASSENGER, "выберите пассажира из списка.");
        }

        String train = b.getTrainNumber();
        if (isBlank(train)) {
            errors.add(TRAIN, "обязательное поле — введите номер поезда.");
        } else if (!train.matches(InputValidator.TRAIN_REGEX)) {
            errors.add(TRAIN, "ожидается 3 цифры и одна заглавная русская буква "
                    + "(кроме Ь, Ъ, Ё, Ы). Пример: 123А, 045Б.");
        }

        validateStation(FROM, b.getRouteFrom(), "станцию отправления", errors);
        validateStation(TO, b.getRouteTo(), "станцию назначения", errors);
        if (!isBlank(b.getRouteFrom()) && !isBlank(b.getRouteTo())
                && b.getRouteFrom().trim().equalsIgnoreCase(b.getRouteTo().trim())) {
            errors.add(TO, "станция назначения не может совпадать со станцией отправления.");
        }

        validateDeparture(b.getDepartureDate(), b.getDepartureTime(), checkDepartureWindow, errors);

        if (b.getWagonNumber() < 1 || b.getWagonNumber() > MAX_WAGON) {
            errors.add(WAGON, "номер вагона должен быть от 1 до " + MAX_WAGON
                    + " (указано " + b.getWagonNumber() + ").");
        }
        if (b.getSeatNumber() < 1 || b.getSeatNumber() > MAX_SEAT) {
            errors.add(SEAT, "номер места должен быть от 1 до " + MAX_SEAT
                    + " (указано " + b.getSeatNumber() + ").");
        }

        validatePrice(b.getPrice(), errors);

        if (b.getStatus() == null) {
            errors.add(STATUS, "выберите статус брони.");
        }
    }

    private static void validateStation(String field, String value, String what,
                                        ValidationErrors errors) {
        if (isBlank(value)) {
            errors.add(field, "обязательное поле — введите " + what + ".");
        } else if (value.length() < STATION_MIN || value.length() > STATION_MAX) {
            errors.add(field, "название должно быть от " + STATION_MIN + " до " + STATION_MAX
                    + " символов (сейчас " + value.length() + ").");
        } else if (!value.matches(STATION_REGEX)) {
            errors.add(field, "допускаются буквы, цифры, пробел, точка, дефис и скобки. "
                    + "Пример: Москва (Ленинградский вокзал).");
        }
    }

    private static void validateDeparture(LocalDate date, LocalTime time,
                                          boolean checkWindow, ValidationErrors errors) {
        if (date == null) {
            errors.add(DATE, "обязательное поле — выберите дату отправления в календаре.");
        }
        if (time == null) {
            errors.add(TIME, "обязательное поле — введите время в формате ЧЧ:ММ, например 08:30.");
        }
        if (!checkWindow || date == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(MAX_DAYS_AHEAD);
        if (date.isBefore(today)) {
            errors.add(DATE, "дата отправления не может быть в прошлом — укажите "
                    + today.format(DATE_FMT) + " или позже.");
        } else if (date.isAfter(maxDate)) {
            errors.add(DATE, "продажа билетов открыта не более чем на год вперёд — укажите дату "
                    + "не позже " + maxDate.format(DATE_FMT) + ".");
        } else if (date.equals(today) && time != null && !time.isAfter(LocalTime.now())) {
            errors.add(TIME, "поезд с отправлением сегодня в " + time
                    + " уже ушёл — укажите более позднее время или другую дату.");
        }
    }

    private static void validatePrice(BigDecimal price, ValidationErrors errors) {
        if (price == null) {
            errors.add(PRICE, "обязательное поле — введите стоимость билета.");
        } else if (price.compareTo(MIN_PRICE) < 0 || price.compareTo(MAX_PRICE) > 0) {
            errors.add(PRICE, "стоимость должна быть от " + MIN_PRICE + " до " + MAX_PRICE
                    + " ₽ (указано " + price.toPlainString() + ").");
        } else if (price.stripTrailingZeros().scale() > 2) {
            errors.add(PRICE, "допускается не более двух знаков после запятой (копейки).");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
