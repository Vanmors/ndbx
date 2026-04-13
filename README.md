# EventHub - NoSQL Database Project

[![EventHub](https://github.com/Vanmors/ndbx/actions/workflows/eventhub.yml/badge.svg)](https://github.com/{your_username}/{your_repo}/actions/workflows/eventhub.yml)

Backend-сервис платформы мероприятий для практического изучения NoSQL баз данных.

## Основные возможности

- **Анонимные сессии** — через cookie `X-Session-Id` (хранятся в Redis с TTL)
- **Регистрация пользователей** — `POST /users`
- **Аутентификация** — `POST /auth/login`
- **Выход из аккаунта** — `POST /auth/logout`
- **Создание событий** — `POST /events` (только авторизованные пользователи)
- **Просмотр всех событий** — `GET /events` с фильтрацией по `title`, пагинацией (`limit`, `offset`)
- **Health-check** — `GET /health` (не создаёт и не обновляет сессию)

### Технологии

- Spring Boot 3.5+
- Redis 8.0 — для сессий (анонимных и авторизованных)
- MongoDB — для хранения пользователей и событий
- Docker + docker-compose — для запуска сервисов
- Makefile — для удобного управления

## Настройка конфигурации
Конфигурациия проекта задаётся через [.env.local](.env.local):  

### Сервер:
- Порт для запуска сервиса: `SERVER_PORT`
- Хост для запуска сервиса: `SERVER_HOST`
### Сессии
- Время жизни сессии в секундах: APP_USER_SESSION_TTL=300

### Redis (для сессий)
- REDIS_HOST
- REDIS_PORT
- REDIS_PASSWORD
- REDIS_DB

### MongoDB (для пользователей и событий)
- MONGODB_DATABASE
- MONGODB_USER
- MONGODB_PASSWORD
- MONGODB_HOST
- MONGODB_PORT

## Запуск проект
Для запуска проекта необходимо выполнить:
```base
make run
```
Для его остановки:
```base
make stop
```

## Помощь

Возникли вопросы? → [@Vanmrkv](https://t.me/Vanmrkv)
