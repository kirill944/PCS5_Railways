# Railway Booking System — Backend (КР №3)

Backend-часть информационной системы «Бронирование железнодорожных билетов».
Проект представляет собой REST API, разработанный на **Spring Boot 3**, работающий с **PostgreSQL**, защищенный с помощью **JWT** и запускаемый через **Docker**.

## Стек технологий
* **Java 21**
* **Spring Boot 3.5.5** (Web, Security, Data JPA, Validation, Actuator)
* **PostgreSQL 17** (основная база данных)
* **Flyway** (миграции базы данных)
* **Hibernate** (ORM, стратегия `ddl-auto: validate`)
* **Docker / Docker Compose** (контейнеризация)
* **SpringDoc OpenAPI / Swagger UI** (документация API)
* **JJWT** (генерация и проверка JWT токенов)
* **Apache Commons CSV** (импорт/экспорт данных)

## Быстрый старт (через Docker)

Для запуска потребуется только установленный **Docker Desktop**.

### 1. Подготовка окружения
1. Склонируйте репозиторий и перейдите в корневую папку проекта.
2. Создайте файл конфигурации `.env` на основе шаблона:
   ```bash
   cp .env.example .env
   ```
*(В Windows можно использовать `copy .env.example .env`)*
3. При необходимости отредактируйте пароли и `JWT_SECRET_BASE64` в файле `.env`.

### 2. Запуск
Убедитесь, что Docker Desktop запущен. Выполните команду в терминале из корня проекта:

**Windows (PowerShell):**
```powershell
powershell -ExecutionPolicy Bypass -File .\backend\start-compose.ps1
```

**Linux / macOS:**
```bash
docker compose --env-file .env -f backend/docker-compose.yml up -d --build
```

Дождитесь, пока оба контейнера перейдут в статус `healthy`. Первая сборка может занять несколько минут.

### 3. Проверка
* **Healthcheck:** `http://localhost:8080/actuator/health`
* **Swagger UI:** `http://localhost:8080/swagger-ui.html`
* **REST API:** `http://localhost:8080/api`
* **PostgreSQL:** `localhost:55432` (порт по умолчанию)

## Первый вход в систему

При первом запуске Flyway создает схему, а `BootstrapService` автоматически регистрирует администратора. Данные берутся из файла `.env`.

* **Логин:** `admin@railway.local` (указан в `BOOTSTRAP_ADMIN_EMAIL`)
* **Пароль:** `Adm1!ChangeMe2026` (указан в `BOOTSTRAP_ADMIN_PASSWORD`)

> **Важно:** При первом входе система потребует сменить временный пароль (`mustChangePassword: true`).

**Как получить токен:**
Выполните `POST /api/auth/login` с логином и паролем. Из ответа скопируйте `accessToken`. В Swagger UI нажмите кнопку **Authorize** и вставьте токен. Теперь вам доступны все защищенные эндпоинты.

## Структура проекта

```text
.
├── .env                        # Переменные окружения (не коммитится)
├── .env.example                # Шаблон переменных
├── README.md                   # Этот файл
├── database/                   # SQL-скрипты из КР1 (опционально)
└── backend/
    ├── Dockerfile              # Многоэтапная сборка Spring Boot
    ├── docker-compose.yml      # Оркестрация БД и приложения
    ├── pom.xml                 # Maven-зависимости
    └── src/main/
        ├── java/ru/mirea/railways/
        │   ├── config/         # Security, OpenAPI, Properties
        │   ├── controller/     # REST-контроллеры
        │   ├── domain/         # JPA-сущности (Booking, Passenger, User)
        │   ├── dto/            # Модели запросов и ответов (API)
        │   ├── error/          # Единый обработчик ошибок
        │   ├── filter/         # Поиск (Specifications), Correlation ID
        │   ├── repository/     # Spring Data JPA репозитории
        │   ├── security/       # JWT, UserDetails, PasswordPolicy
        │   └── service/        # Бизнес-логика
        └── resources/
            ├── application.yml # Конфигурация Spring
            └── db/migration/   # Flyway миграции (V1, V2, V3)
```

## Реализованный функционал (по ТЗ КР №3)

* **Авторизация и безопасность:**
    * JWT (Access + Refresh), серверные сессии обновления.
    * Хеширование паролей BCrypt.
    * Защита от брутфорса (блокировка после 5 неудачных попыток).
    * Роли: `VIEWER` (чтение), `EDITOR` (редактирование), `ADMIN` (полный доступ).
* **Управление бронированиями (Booking):**
    * CRUD, оптимистичная блокировка (`@Version`).
    * Архивирование и восстановление (мягкое удаление).
    * Фильтрация, поиск, сортировка, пагинация (через `Specification API`).
    * Вычисляемое поле «Уровень риска» (на основе даты и статуса).
* **Справочник пассажиров:** CRUD, включение/отключение.
* **Заметки:** Добавление, редактирование, закрепление заметок к брони.
* **Массовые операции:** Архивация, смена статуса, теги для списка UUID.
* **Импорт/Экспорт CSV:** Экспорт по фильтру, 2-этапный импорт (Preview -> Apply).
* **Аналитика:** Сводные данные по статусам, пассажирам, среднему чеку.
* **Администрирование:** Управление пользователями, сброс паролей.
* **Аудит:** Логирование действий с привязкой к Correlation ID.
* **Swagger UI:** Полная интерактивная документация API.

## Тестирование
В проекте настроены базовые тесты (Smoke-тест контекста, тесты политики паролей, бизнес-логики).
Для запуска тестов локально (требуется Maven):
```bash
cd backend
mvn test
```

## Остановка и очистка

Остановить контейнеры (данные БД сохранятся):
```bash
docker compose --env-file .env -f backend/docker-compose.yml down
```

**Полный сброс (удаление базы данных и всех данных):**
Используется, если вы изменили Flyway-миграции или хотите начать с чистого листа:
```bash
docker compose --env-file .env -f backend/docker-compose.yml down -v
docker volume rm railway_pgdata
```