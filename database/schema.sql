--  Railway Booking System — схема базы данных
--  СУБД: PostgreSQL 14+
--  Файл: schema.sql

-- Удаляем БД, если она уже существует (для чистого пересоздания)
DROP DATABASE IF EXISTS railway_booking;

-- Создаём базу данных
CREATE DATABASE railway_booking
    WITH ENCODING 'UTF8'
    LC_COLLATE = 'ru_RU.UTF-8'
    LC_CTYPE   = 'ru_RU.UTF-8'
    TEMPLATE   = template0;

-- Подключаемся к созданной БД
\connect railway_booking;

--  Таблица: passengers
--  Хранит информацию о пассажирах
DROP TABLE IF EXISTS bookings CASCADE;
DROP TABLE IF EXISTS passengers CASCADE;

CREATE TABLE passengers (
                            id               BIGSERIAL    PRIMARY KEY,
                            full_name        VARCHAR(150) NOT NULL,
                            passport_number  VARCHAR(20)  NOT NULL,
                            email            VARCHAR(100) NOT NULL,
                            phone            VARCHAR(20)  NOT NULL,
                            birth_date       DATE         NOT NULL,

    -- Ограничения
                            CONSTRAINT uq_passengers_passport UNIQUE (passport_number),
                            CONSTRAINT uq_passengers_email    UNIQUE (email),
                            CONSTRAINT chk_passengers_email   CHECK (email LIKE '%@%.%'),
                            CONSTRAINT chk_passengers_phone   CHECK (phone ~ '^\+?[0-9]{10,15}$'),
    CONSTRAINT chk_passengers_birth   CHECK (birth_date < CURRENT_DATE)
);

-- Индексы для ускорения поиска
CREATE INDEX idx_passengers_full_name ON passengers (full_name);
CREATE INDEX idx_passengers_email     ON passengers (email);

-- Комментарии
COMMENT ON TABLE  passengers                 IS 'Пассажиры системы';
COMMENT ON COLUMN passengers.passport_number IS 'Уникальный номер паспорта';
COMMENT ON COLUMN passengers.email           IS 'Уникальный email пассажира';

--  Таблица: bookings
--  Основная сущность — бронирование билета
CREATE TABLE bookings (
                          id               BIGSERIAL     PRIMARY KEY,
                          passenger_id     BIGINT        NOT NULL,
                          train_number     VARCHAR(10)   NOT NULL,
                          route_from       VARCHAR(100)  NOT NULL,
                          route_to         VARCHAR(100)  NOT NULL,
                          departure_date   DATE          NOT NULL,
                          departure_time   TIME          NOT NULL,
                          wagon_number     INT           NOT NULL,
                          seat_number      INT           NOT NULL,
                          price            NUMERIC(10,2) NOT NULL,
                          status           VARCHAR(20)   NOT NULL,
                          created_at       TIMESTAMP     NOT NULL DEFAULT NOW(),

    -- Внешний ключ на пассажира
                          CONSTRAINT fk_bookings_passenger
                              FOREIGN KEY (passenger_id)
                                  REFERENCES passengers (id)
                                  ON DELETE CASCADE
                                  ON UPDATE CASCADE,

    -- Проверки целостности
                          CONSTRAINT chk_bookings_wagon    CHECK (wagon_number >= 1),
                          CONSTRAINT chk_bookings_seat     CHECK (seat_number  >= 1),
                          CONSTRAINT chk_bookings_price    CHECK (price > 0),
                          CONSTRAINT chk_bookings_status   CHECK (status IN
                                                                  ('CREATED', 'CONFIRMED', 'PAID', 'COMPLETED', 'CANCELLED')),
                          CONSTRAINT chk_bookings_route    CHECK (route_from <> route_to),

    -- Уникальность места на конкретный поезд, вагон и дату
                          CONSTRAINT uq_bookings_seat UNIQUE
                              (train_number, wagon_number, seat_number, departure_date)
);

-- Индексы для фильтрации и сортировки
CREATE INDEX idx_bookings_passenger      ON bookings (passenger_id);
CREATE INDEX idx_bookings_status         ON bookings (status);
CREATE INDEX idx_bookings_departure_date ON bookings (departure_date);
CREATE INDEX idx_bookings_train_number   ON bookings (train_number);
CREATE INDEX idx_bookings_route          ON bookings (route_from, route_to);

-- Комментарии
COMMENT ON TABLE  bookings                IS 'Бронирования железнодорожных билетов';
COMMENT ON COLUMN bookings.passenger_id   IS 'FK на passengers.id';
COMMENT ON COLUMN bookings.train_number   IS 'Номер поезда (например, 123А)';
COMMENT ON COLUMN bookings.status         IS 'Статус брони (enum BookingStatus)';
COMMENT ON COLUMN bookings.created_at     IS 'Дата и время создания брони';