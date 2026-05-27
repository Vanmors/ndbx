# EventHub - NoSQL Database Project

[![EventHub](https://github.com/Vanmors/ndbx/actions/workflows/eventhub.yml/badge.svg)](https://github.com/Vanmors/ndbx/actions/workflows/eventhub.yml)

Backend-сервис платформы мероприятий для практического изучения NoSQL баз данных.

## Основные возможности

- **Анонимные сессии** — через cookie `X-Session-Id` (хранятся в Redis с TTL)
- **Регистрация пользователей** — `POST /users`
- **Аутентификация** — `POST /auth/login`, `POST /auth/logout`
- **Управление мероприятиями** — создание, просмотр, обновление, фильтрация
- **Реакции на мероприятия** — лайки и дизлайки (`POST /events/{id}/like`, `POST /events/{id}/dislike`)
- **Отзывы на мероприятия** — создание, просмотр, редактирование (`/events/{id}/reviews`)
- **Кэширование реакций и отзывов** — Redis (Cache-Aside) + Cassandra как основное хранилище
- **Health-check** — `GET /health`

## Технологии

- Spring Boot 3.5+
- MongoDB 8.0 — шардированный кластер для хранения пользователей и мероприятий
- Redis 8.0 — сессии, кэш реакций и отзывов
- Apache Cassandra 4.1 — хранение реакций (лайков/дизлайков) и отзывов
- Docker + docker-compose
- Makefile

## Архитектура БД

### MongoDB — шардированный кластер

- 2 шарда по 3 реплики (shard1a/b/c, shard2a/b/c)
- Config-сервер (cfg1)
- Query router (mongos)
- Коллекция `events` шардирована по `created_by` (hashed)

### Redis

- Хранение сессий (`sid:{id}`) с TTL
- Кэш реакций (`events:{md5(title)}:reactions`) с TTL
- Кэш отзывов (`event:{md5(title)}:reviews`) с TTL

### Cassandra

- Таблица `event_reactions` (keyspace: `testkeyspace`)
  - Partition key: `event_id`, clustering key: `created_by`
  - Поля: `event_id`, `created_by`, `like_value` (1 — лайк, -1 — дизлайк), `created_at`
- Таблица `event_reviews` (keyspace: `testkeyspace`)
  - Partition key: `event_id`, clustering keys: `created_at` DESC, `id`
  - Поля: `event_id`, `id` (UUID), `rating` (1-5), `comment`, `created_by`, `created_at`, `updated_at`

## API

### Сессии и аутентификация

| Метод | Эндпоинт | Описание |
|-------|----------|----------|
| POST | `/session` | Создать/обновить сессию |
| POST | `/auth/login` | Войти в аккаунт |
| POST | `/auth/logout` | Выйти из аккаунта |
| GET | `/health` | Health-check |

### Пользователи

| Метод | Эндпоинт | Описание |
|-------|----------|----------|
| POST | `/users` | Регистрация |
| GET | `/users` | Список пользователей (фильтры: `name`, `id`) |
| GET | `/users/{id}` | Пользователь по ID |
| GET | `/users/{id}/events` | Мероприятия пользователя (`?include=reactions,reviews`) |

### Мероприятия

| Метод | Эндпоинт | Описание |
|-------|----------|----------|
| POST | `/events` | Создать мероприятие |
| GET | `/events` | Список мероприятий (фильтры: `title`, `category`, `city`, `price_from/to`, `date_from/to`) |
| GET | `/events/{id}` | Мероприятие по ID (`?include=reactions,reviews`) |
| PATCH | `/events/{id}` | Обновить мероприятие |
| POST | `/events/{id}/like` | Лайк |
| POST | `/events/{id}/dislike` | Дизлайк |
| POST | `/events/{id}/reviews` | Оставить отзыв (один пользователь — один отзыв) |
| GET | `/events/{id}/reviews` | Список отзывов (пагинация: `limit`, `offset`) |
| PATCH | `/events/{id}/reviews/{review_id}` | Редактировать отзыв (только владелец) |

Параметры пагинации: `limit` (по умолчанию 10), `offset` (по умолчанию 0).

## Настройка конфигурации

Конфигурация задаётся через [.env.local](.env.local):

### Приложение
- `APP_PORT` — порт сервиса
- `APP_HOST` — хост сервиса
- `APP_USER_SESSION_TTL` — TTL сессии (сек)
- `APP_LIKE_TTL` — TTL кэша реакций (сек)
- `APP_EVENT_REVIEWS_TTL` — TTL кэша отзывов (сек, по умолчанию 120)

### Redis
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_DB`

### MongoDB
- `MONGODB_HOST`, `MONGODB_PORT`, `MONGODB_DATABASE`, `MONGODB_USER`, `MONGODB_PASSWORD`
- `MONGO_CFG_PORT`, `MONGO_SHARD1A_PORT` .. `MONGO_SHARD2C_PORT`

### Cassandra
- `CASSANDRA_HOSTS`, `CASSANDRA_PORT`, `CASSANDRA_KEYSPACE`
- `CASSANDRA_USERNAME`, `CASSANDRA_PASSWORD`
- `CASSANDRA_CONSISTENCY`, `CASSANDRA_LOCAL_DATACENTER`

## Документация API

После запуска проекта доступна автогенерируемая документация:

- **Swagger UI**: `http://localhost:8081/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8081/v3/api-docs`

## Запуск

```bash
make run       # Собрать и запустить все сервисы (detached)
make rund      # Запустить с выводом логов
make stop      # Остановить сервисы
make clean     # Остановить и удалить volumes
make services  # Статус сервисов
```

## Помощь

Возникли вопросы? → [@Vanmrkv](https://t.me/Vanmrkv)

