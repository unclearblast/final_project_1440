## OrbitaMarket – Платформа заказов спутниковых продуктов

OrbitaMarket – учебный проект, реализующий ядро платформы продажи спутниковых продуктов.
Проект состоит из двух микросервисов (**Payments Service**, **Orders Service**), API Gateway и инфраструктурных сервисов (Kafka, PostgreSQL).
Взаимодействие между сервисами организовано асинхронно через брокер сообщений.

## Оглавление
- [Архитектура проекта](§архитектура-проекта)
- [Технологический стек](§технологический-стек)
- [Быстрый старт](§быстрый-старт)
  - [Запуск инфраструктуры (Docker Compose)](§запуск-инфраструктуры-docker-compose)
  - [Сборка и запуск микросервисов](§сборка-и-запуск-микросервисов)
- [API Reference](§api-reference)
  - [Payments Service](§payments-service)
  - [Orders Service](§orders-service)
  - [API Gateway](§api-gateway)
- [Асинхронное взаимодействие (Kafka)](§асинхронное-взаимодействие-kafka)
- [Тестирование](§тестирование)
  - [Чек-лист сценариев](§чек-лист-сценариев)
  - [Автотесты (RestAssured + Allure)](§автотесты-restassured--allure)
- [Аналитические запросы](§аналитические-запросы)
- [Информационная безопасность](§информационная-безопасность)
- [Планирование и Roadmap](§планирование-и-roadmap)
- [Лицензия](§лицензия)

## Архитектура проекта

Проект соответствует архитектурному стилю микросервисов.  
Схема контекста (C1) и контейнеров (C2) приведена в файлах `docs/c4/c1-context.puml` и `docs/c4/c2-containers.puml`.

**Компоненты:**
- **API Gateway** (Spring Cloud Gateway, порт 8082) – единая точка входа, маршрутизация `/api/v1/payments/**` → Payments Service, `/api/v1/orders/**` → Orders Service, проброс заголовка `X-User-Id`.
- **Payments Service** (Spring Boot, порт 8080) – управление счетами (создание, пополнение, баланс), асинхронное списание средств через обработку событий Kafka, идемпотентность.
- **Orders Service** (Spring Boot, порт 8081) – управление заказами (создание, список, статус), публикация событий `OrderPaymentRequested` через Outbox, обновление статусов при получении результатов оплаты.
- **PostgreSQL** (две схемы/базы: `orders_db` на порту 5432, `payments_db` на порту 5433) – изоляция данных между сервисами.
- **Kafka** (порт 9092) – брокер сообщений для асинхронного взаимодействия.

## Технологический стек
- Java 21
- Spring Boot 3.2.5
- Spring Data JPA, PostgreSQL
- Spring Cloud Gateway
- Apache Kafka (Confluent 7.5.0)
- Docker, Docker Compose
- Maven
- JUnit 5, RestAssured, Allure (тестирование)
- Gitleaks, Semgrep, OSV-Scanner (безопасность)

## Быстрый старт

### Проверка работы (Happy Path)

После запуска всех сервисов выполните:

```bash
# 1. Создание счёта
curl -s -X POST http://localhost:8082/api/v1/payments/accounts \
  -H "X-User-Id: test-user" | jq .

# 2. Пополнение баланса
curl -s -X POST http://localhost:8082/api/v1/payments/accounts/top-up \
  -H "X-User-Id: test-user" \
  -H "Content-Type: application/json" \
  -d '{"amount":1000}' | jq .

# 3. Создание заказа (ARCHIVE, цена 120)
ORDER_ID=$(curl -s -X POST http://localhost:8082/api/v1/orders \
  -H "X-User-Id: test-user" \
  -H "Content-Type: application/json" \
  -d '{"productType":"ARCHIVE","price":120,"payload":"{\"aoi\":\"POLYGON((...))\"}"}' | jq -r '.orderId')

echo "Заказ создан: $ORDER_ID"

# 4. Пауза для асинхронной обработки (OutboxPublisher срабатывает каждые 5 секунд)
sleep 10

# 5. Проверка статуса заказа
curl -s http://localhost:8082/api/v1/orders/$ORDER_ID \
  -H "X-User-Id: test-user" | jq .

# 6. Проверка баланса (должен уменьшиться на 120)
curl -s http://localhost:8082/api/v1/payments/accounts/balance \
  -H "X-User-Id: test-user" | jq .


### Запуск инфраструктуры (Docker Compose)

Убедитесь, что Docker установлен и запущен.  
В корне проекта выполните:

sudo systemctl start docker   # если ещё не запущен
cd ~/OrbitaMarket
sudo docker-compose down -v   # очистка старых данных (опционально)
sudo docker-compose up -d
Проверьте доступность портов:

bash
nc -zv localhost 9092   # Kafka
nc -zv localhost 5432   # orders_db
nc -zv localhost 5433   # payments_db



### Сборка и запуск микросервисов

Каждый сервис собирается и запускается отдельно. Команды для фонового запуска:

bash
# Payments Service
cd ~/OrbitaMarket/payments-service
mvn clean package -DskipTests
mvn spring-boot:run > /tmp/payments.log 2>&1 &

# Orders Service
cd ~/OrbitaMarket/orders-service
mvn clean package -DskipTests
mvn spring-boot:run > /tmp/orders.log 2>&1 &

# API Gateway
cd ~/OrbitaMarket/gateway
mvn clean package -DskipTests
mvn spring-boot:run > /tmp/gateway.log 2>&1 &
После запуска всех трёх сервисов (ожидание ~30 секунд) можно обращаться к API через Gateway на порту 8082.

# Общие DTO (common-dtos)

Для исключения дублирования и ошибок сериализации все события, передаваемые через Kafka, вынесены в отдельный модуль **common-dtos**.  
Классы событий (`OrderPaymentRequestedEvent`, `OrderPaymentCompletedEvent`, `OrderPaymentFailedEvent`) находятся в пакете `com.orbitamarket.common.dto` и используются обоими сервисами.

Модуль подключается в `payments-service` и `orders-service` как обычная зависимость Maven.

## API Reference

### Payments Service

Базовый путь: /api/v1/payments (через Gateway: /api/v1/payments)

#### 1. Создание счёта

http
POST /api/v1/payments/accounts
Header: X-User-Id: {user_id}
Ответ: 200 OK с существующим счётом (идемпотентно) или новый счёт с балансом 0.

Пример ответа:

json
{ "userId": "user-1", "balance": 0, "currency": "geocredits" }
#### 2. Пополнение счёта

http
POST /api/v1/payments/accounts/top-up
Header: X-User-Id: {user_id}
Content-Type: application/json

{ "amount": 1000 }
amount – положительное целое число.

Ошибки: 400 INVALID_AMOUNT (если ≤ 0), 404 ACCOUNT_NOT_FOUND, 400 MISSING_USER_ID.

#### 3. Запрос баланса

http
GET /api/v1/payments/accounts/balance
Header: X-User-Id: {user_id}
Ответ: 200 OK, JSON с полями userId, balance, currency.

Ошибки: 404 ACCOUNT_NOT_FOUND, 400 MISSING_USER_ID.

### Orders Service

Базовый путь: /api/v1/orders

#### 1. Создание заказа

http
POST /api/v1/orders
Header: X-User-Id: {user_id}
Content-Type: application/json

{
  "productType": "ARCHIVE",
  "price": 120,
  "payload": "{\"aoi\":\"POLYGON((...))\"}"
}
Поддерживаемые типы: ARCHIVE, TASKING, MONITORING.

Ответ: 201 CREATED, статус PAYMENT_PENDING, orderId.

Ошибки: 400 INVALID_PAYLOAD, 400 INVALID_PRICE, 400 UNKNOWN_PRODUCT_TYPE, 400 MISSING_USER_ID.

#### 2. Список заказов пользователя

http
GET /api/v1/orders
Header: X-User-Id: {user_id}
Ответ: 200 OK, массив заказов.

#### 3. Получение заказа по ID

http
GET /api/v1/orders/{orderId}
Header: X-User-Id: {user_id}
Ответ: 200 OK с деталями заказа.

Ошибки: 404 ORDER_NOT_FOUND, 403 ACCESS_DENIED (если чужой заказ).

### API Gateway

Проксирует запросы:

/api/v1/payments/** → http://localhost:8080

/api/v1/orders/** → http://localhost:8081

Заголовок X-User-Id передаётся автоматически. Дополнительной конфигурации не требуется.

## Асинхронное взаимодействие (Kafka)

Сценарий оплаты:

Orders Service создаёт заказ и сохраняет событие OrderPaymentRequested в таблицу outbox (в той же транзакции).

Планировщик OutboxPublisher с интервалом 5 секунд читает неотправленные записи и публикует их в топик order.payment.requested, сериализуя объект в JSON.

Payments Service подписан на этот топик. Обработчик PaymentProcessor принимает сообщение, проверяет идемпотентность (по event_id через таблицу inbox), выполняет списание (optimistic locking), и публикует результат в топик order.payment.completed или order.payment.failed.

Orders Service подписан на оба топика; при получении результата обновляет статус заказа.

Имена топиков:

order.payment.requested

order.payment.completed

order.payment.failed

Гарантии доставки:

Transactional Outbox в Orders Service (публикация в одной транзакции с заказом).

Inbox в Payments Service (запись event_id перед обработкой; дубликаты игнорируются).

Идемпотентность списания: повторная обработка того же order_id не уменьшает баланс повторно.

Оптимистическая блокировка (поле version у счёта) для конкурентных операций.

## Тестирование

### Чек-лист сценариев

Файл: docs/test-checklist.md (15 сценариев). Основные проверки:

Happy path: пополнение 1000 → заказ на 120 → статус PAID, баланс 880.

Недостаточно средств → PAYMENT_FAILED, баланс не меняется.

Идемпотентность списания и создания счёта.

Конкурентные заказы (неотрицательный баланс).

Валидация запросов (неверные поля, отсутствующие заголовки).

### Автотесты (RestAssured + Allure)

Отдельный репозиторий: ~/orbita-market-tests.
Включает 14 тестов, покрывающих все эндпоинты через Gateway.
Генерация отчёта:

bash
cd ~/orbita-market-tests
mvn clean test
mvn allure:report
allure serve target/allure-report
## Аналитические запросы

Файл docs/analytics.sql содержит запрос для получения суммы и количества оплаченных заказов по пользователям:

sql
SELECT user_id, COUNT(*) AS paid_orders_count, SUM(price) AS total_spent_geocredits
FROM orders WHERE status = 'PAID'
GROUP BY user_id ORDER BY total_spent_geocredits DESC;
## Информационная безопасность

Gitleaks – поиск секретов в репозитории (gitleaks detect --report-format json ...).

Semgrep – SAST с использованием локальных правил (semgrep-rules). Отчёт: semgrep-report.json.

OSV-Scanner – анализ зависимостей (pom.xml) на известные уязвимости. Отчёт: osv-report.json.

Таблица триажа – файл docs/triage.md (и LaTeX-версия docs/triage.tex), включает более 20 находок с классификацией TP/FP/риск и рекомендациями по исправлению.

## Планирование и Roadmap

См. PROJECT.md. Ключевые вехи:

Sprints 0–3: реализация микросервисов и Gateway.

Sprint 4: тестирование, документация, аналитика, C4-диаграммы.

Sprint 5: сканирование безопасности, таблица триажа.

MVP достигнут 05.07.2026.

## Лицензия

Данный проект является учебным и не подлежит коммерческому использованию.
