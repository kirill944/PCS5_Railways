## Создание таблицы "Пассажиры"

```sql
CREATE TABLE passengers (
    id              BIGSERIAL PRIMARY KEY,
    full_name       VARCHAR(150) NOT NULL,
    passport_number VARCHAR(20)  NOT NULL UNIQUE,
    email           VARCHAR(100) NOT NULL UNIQUE,
    phone           VARCHAR(20)  NOT NULL,
    birth_date      DATE         NOT NULL,
    CONSTRAINT chk_birth_date CHECK (birth_date < CURRENT_DATE)
);
```

## Создание таблицы "Бронирования"

```sql
CREATE TABLE bookings
(
    id             BIGSERIAL PRIMARY KEY,
    passenger_id   BIGINT         NOT NULL,
    train_number   VARCHAR(10)    NOT NULL,
    route_from     VARCHAR(100)   NOT NULL,
    route_to       VARCHAR(100)   NOT NULL,
    departure_date DATE           NOT NULL,
    departure_time TIME           NOT NULL,
    wagon_number   INT            NOT NULL,
    seat_number    INT            NOT NULL,
    price          NUMERIC(10, 2) NOT NULL,
    status         VARCHAR(20)    NOT NULL,
    created_at     TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_booking_passenger
        FOREIGN KEY (passenger_id) REFERENCES passengers (id)
            ON DELETE CASCADE,

    CONSTRAINT chk_wagon CHECK (wagon_number >= 1),
    CONSTRAINT chk_seat CHECK (seat_number >= 1),
    CONSTRAINT chk_price CHECK (price > 0),
    CONSTRAINT chk_status CHECK (status IN
                                 ('CREATED', 'CONFIRMED', 'PAID', 'COMPLETED', 'CANCELLED')),

    CONSTRAINT uq_seat UNIQUE
        (train_number, wagon_number, seat_number, departure_date)
);
```