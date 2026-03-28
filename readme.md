<div align="center">

# ⚙️ CoreLedgerEngine

**A production-grade financial ledger backend** implementing double-entry bookkeeping with a blockchain-inspired tamper-proof audit trail.

Built with Java 17, Spring Boot 3, PostgreSQL, Redis, and Docker — designed for horizontal scaling behind an Nginx load balancer.

[![CI Pipeline](https://github.com/mayank1008-tech/CoreLedgerEngine/actions/workflows/ci.yml/badge.svg)](https://github.com/mayank1008-tech/CoreLedgerEngine/actions)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-Alpine-DC382D?logo=redis&logoColor=white)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://docs.docker.com/compose/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

</div>

---

## 📋 Table of Contents

- [Features](#-features)
- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [Getting Started](#-getting-started)
- [API Reference](#-api-reference)
- [Engineering Deep Dives](#-engineering-deep-dives)
  - [Double-Entry Bookkeeping](#-double-entry-bookkeeping)
  - [Idempotency via Reference ID](#-idempotency-via-reference-id)
  - [Optimistic Locking with Retry](#-optimistic-locking-with-retry)
  - [Blockchain-Style Hash Chaining](#-blockchain-style-hash-chaining)
  - [Distributed Rate Limiting](#-distributed-rate-limiting)
  - [Redis Caching with Security](#-redis-caching-with-security)
  - [JWT Auth via HttpOnly Cookies](#-jwt-auth-via-httponly-cookies)
  - [Role-Based Access Control](#-role-based-access-control)
  - [Paginated Account Statements](#-paginated-account-statements)
  - [Input Validation and Error Handling](#-input-validation-and-error-handling)
- [Security Summary](#-security-summary)
- [Rate Limiting](#-rate-limiting)
- [Testing](#-testing)
- [CI/CD Pipeline](#-cicd-pipeline)
- [Docker Services](#-docker-services)
- [Project Structure](#-project-structure)
- [Configuration](#-configuration)
- [Author](#-author)

---

## ✨ Features

| Feature | Description |
|---|---|
| 🏦 **Double-Entry Bookkeeping** | Every transaction creates balanced debit and credit entries — the foundation of real-world accounting |
| 🔁 **Idempotency** | Unique `referenceId` per transaction prevents double-spending under network retries or duplicate requests |
| 🔒 **Optimistic Locking** | `@Version`-based concurrency control with `@Retryable` automatic retry — no pessimistic locks, no throughput bottlenecks |
| ⛓️ **Blockchain Audit Trail** | SHA-256 hash-chained ledger entries create a tamper-evident, forensically auditable history |
| 🛡️ **Distributed Rate Limiting** | Per-user Bucket4j token buckets synced via Redis — limits hold correctly across all app instances |
| ⚡ **Redis Balance Caching** | Balance lookups cached with 10-min TTL, owner-ID verification on every cache hit, graceful DB fallback |
| 🔐 **JWT Authentication** | Stateless auth via `HttpOnly; Secure; SameSite=Strict` cookies — tokens never exposed to JavaScript |
| 👮 **Role-Based Access Control** | `ROLE_USER` and `ROLE_ADMIN` enforced at method level via `@PreAuthorize` |
| 📊 **Paginated Statements** | Account history with configurable sort, pagination, and automatic CREDIT/DEBIT classification |
| ✅ **Input Validation** | Bean Validation on all request DTOs with a global `@RestControllerAdvice` — no stack traces ever reach clients |
| 🐳 **Multi-Instance Deployment** | Multi-stage Dockerfile, Compose orchestration, Nginx round-robin load balancer across two app instances |
| 🤖 **CI/CD** | GitHub Actions: test → build → artifact upload, triggered on every push and pull request |
| 📖 **Swagger UI** | Auto-generated interactive API documentation via SpringDoc OpenAPI |

---

## 🏗️ Architecture

```
                      ┌──────────────────────┐
                      │        Client        │
                      │  (Browser / Postman) │
                      └──────────┬───────────┘
                                 │
                      ┌──────────▼───────────┐
                      │    Nginx  (Port 80)  │
                      │    Load Balancer     │
                      │    Round-Robin       │
                      └────────┬──────┬──────┘
                               │      │
                   ┌───────────▼──┐ ┌─▼────────────┐
                   │   App #1     │ │    App #2    │
                   │  Port 8081   │ │   Port 8082  │
                   │ Spring Boot  │ │ Spring Boot  │
                   │ + JWT Auth   │ │ + JWT Auth   │
                   └──────┬───┬───┘ └──┬────┬──────┘
                          │   │        │    │
              ┌───────────▼───▼────────▼────▼──────────┐
              │                                         │
       ┌──────▼──────┐                       ┌──────────▼──────┐
       │  PostgreSQL  │                       │     Redis       │
       │   Port 5432  │                       │   Port 6379     │
       │              │                       │                 │
       │ • users      │                       │ • Balance cache │
       │ • accounts   │                       │ • Rate limit    │
       │ • ledger     │                       │   buckets       │
       │ • txns       │                       │                 │
       └──────────────┘                       └─────────────────┘
```

### Layered application design

```
Controllers  →  Rate Limit Check  →  Services  →  Repositories  →  PostgreSQL
                                          ↕
                                        Redis
                                   (cache + buckets)
```

- **Controllers** — HTTP layer, authentication extraction, rate limit enforcement
- **Services** — all business logic: double-entry writes, hash chaining, cache management, ownership checks
- **Repositories** — Spring Data JPA interfaces, zero boilerplate
- **Redis** — balance cache with owner-ID verification + distributed rate limit token buckets

---

## 🛠️ Tech Stack

| Layer | Technology | Version | Purpose |
|---|---|---|---|
| Language | Java | 17 LTS | Core runtime |
| Framework | Spring Boot | 3.3.5 | Application framework |
| Security | Spring Security + JJWT | 0.13 | Authentication and authorization |
| ORM | Spring Data JPA / Hibernate | — | Database access, optimistic locking |
| Database | PostgreSQL | 15-alpine | Primary ACID-compliant store |
| Cache | Redis + Lettuce | Alpine | Balance caching and rate limit state |
| Rate Limiting | Bucket4j | 8.16 | Distributed token-bucket algorithm |
| Retry | Spring Retry | — | Automatic retry on optimistic lock failures |
| API Docs | SpringDoc OpenAPI | 2.6.0 | Swagger UI generation |
| DTO Mapping | ModelMapper | 3.2.4 | Entity ↔ DTO conversion |
| Build | Maven + Wrapper | — | Reproducible dependency management |
| Containerisation | Docker (multi-stage) | — | Lean production images |
| Orchestration | Docker Compose | — | Multi-service local deployment |
| Load Balancer | Nginx | Alpine | Round-robin traffic distribution |
| CI/CD | GitHub Actions | — | Automated test and build pipeline |
| Testing | JUnit 5 + Mockito | — | Unit and service layer tests |
| Logging | LogBack | Slf4j | Debug level console logging |

---

## 🚀 Getting Started

### Prerequisites

- [Docker](https://docs.docker.com/get-docker/) and [Docker Compose](https://docs.docker.com/compose/install/)
- *(Optional for local dev)* Java 17 + Maven

### Option 1: Docker Compose — recommended

```bash
# Clone the repository
git clone https://github.com/mayank1008-tech/CoreLedgerEngine.git
cd CoreLedgerEngine

# Start the full stack: 2 app instances, PostgreSQL, Redis, Nginx, pgAdmin
docker compose up --build
```

| URL | Service |
|---|---|
| `http://localhost` | Application (via Nginx load balancer) |
| `http://localhost:8081` | App instance 1 (direct) |
| `http://localhost:8082` | App instance 2 (direct) |
| `http://localhost/swagger-ui.html` | Interactive API documentation |
| `http://localhost:5050` | pgAdmin — database UI (`admin@admin.com` / `admin`) |

### Option 2: Local development

```bash
# Start only the infrastructure dependencies
docker compose up postgres redis -d

# Build and run
./mvnw spring-boot:run

# Run tests
./mvnw clean test
```

### Default credentials (auto-seeded on first startup)

| Field | Value |
|---|---|
| Username | `systemAdmin` |
| Password | `pass123` |
| Roles | `ROLE_ADMIN` + `ROLE_USER` |

> `CENTRAL_BANK` account is also created automatically. It is the system counterparty for all deposits and withdrawals — no manual setup required.

### Quick smoke test

```bash
# 1. Register
curl -X POST http://localhost/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "pass123", "email": "alice@test.com"}'

# 2. Login — copy the JWT token from the response
curl -X POST http://localhost/api/auth/signin \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "pass123"}'

# 3. Create an account
curl -X POST http://localhost/api/account/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -d '{"accountName": "AliceSavings", "currency": "INR"}'
```

---

## 📡 API Reference

Full interactive documentation at `http://localhost/swagger-ui.html`

### Authentication

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/signup` | ❌ | Register a new user |
| `POST` | `/api/auth/signin` | ❌ | Login and receive JWT token |
| `POST` | `/api/auth/signout` | ✅ | Logout, clear cookie |
| `GET` | `/api/auth/user` | ✅ | Get current user details |
| `GET` | `/api/auth/username` | ✅ | Get current username |

### Banking Operations

| Method | Endpoint | Auth | Rate Limit | Description |
|---|---|---|---|---|
| `POST` | `/api/account/create` | ✅ | General | Create a new bank account |
| `GET` | `/api/account/list` | ✅ | General | List all your accounts |
| `GET` | `/api/account/balance/{id}` | ✅ | General | Get balance (Redis cached) |
| `POST` | `/api/deposit` | ✅ | Transaction | Deposit funds from CENTRAL_BANK |
| `POST` | `/api/withdraw` | ✅ | Transaction | Withdraw funds to CENTRAL_BANK |
| `POST` | `/api/transfer` | ✅ | Transaction | Transfer between two accounts |
| `GET` | `/api/statement/{id}` | ✅ | General | Paginated account statement |

### Administration

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/admin/audit/{accountId}` | ✅ `ROLE_ADMIN` | Verify ledger hash chain integrity |

### Example: transfer money

```bash
curl -X POST http://localhost/api/transfer \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -d '{
    "fromAccountId": "550e8400-e29b-41d4-a716-446655440000",
    "toAccountId":   "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
    "amount":        1500.00,
    "referenceId":   "TXN-20260315-001"
  }'
```

```json
{ "message": "Transfer successful", "status": true }
```

<details>
<summary><b>GET /api/statement/{accountId} — example response</b></summary>

**Query params:** `?pageNumber=0&pageSize=10&sortBy=loggedAt&sortOrder=desc`

```json
{
  "content": [
    {
      "amount": 1500.00,
      "date": "2026-03-15T10:30:00",
      "referenceId": "TXN-20260315-001",
      "type": "CREDIT"
    },
    {
      "amount": -500.00,
      "date": "2026-03-14T15:20:00",
      "referenceId": "TXN-20260314-002",
      "type": "DEBIT"
    }
  ],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 47,
  "totalPages": 5,
  "lastPage": false
}
```
</details>

<details>
<summary><b>GET /api/admin/audit/{accountId} — example responses</b></summary>

```json
{ "status": "VALID",                     "brokenAt": null }
{ "status": "CORRUPTED: DATA MODIFIED",  "brokenAt": "ledger-entry-uuid" }
{ "status": "CORRUPTED: BROKEN CHAIN",   "brokenAt": "ledger-entry-uuid" }
```
</details>

---

## 🔬 Engineering Deep Dives

These are the non-trivial decisions in the project — the things worth explaining in an interview.

---

### 🏦 Double-Entry Bookkeeping

Every financial operation creates **exactly two ledger entries** — a debit and a credit. This is enforced at the code level inside a single `@Transactional` method; there is no code path that writes one entry without the other. If either write fails, the entire transaction rolls back.

```
Transfer ₹1,500 from Alice → Bob

  LedgerEntry #1 │ account = Alice │ amount = −1,500 │ DEBIT
  LedgerEntry #2 │ account = Bob   │ amount = +1,500 │ CREDIT

  Sum across all entries = 0  ✅  Money moved, not created.
```

- **Deposits** → `CENTRAL_BANK → User Account`
- **Withdrawals** → `User Account → CENTRAL_BANK`
- **Transfers** → `Account A → Account B`

Every operation flows through the same double-entry path, including deposits and withdrawals. The system's total balance across all accounts always sums to zero — a fundamental accounting invariant that is never violated.

---

### 🔁 Idempotency via Reference ID

Every transaction request carries a client-supplied `referenceId`. A `UNIQUE` database constraint on this column means the same transaction can never be processed twice, regardless of retries or duplicate button clicks.

```
Request #1  →  referenceId: "TXN-2026-001"  →  ✅ Processed, money moved
Request #2  →  referenceId: "TXN-2026-001"  →  ❌ HTTP 409 Conflict
```

The check happens at two levels: an application-level lookup before processing (fast fail with a clear error), and the database `UNIQUE` constraint as an iron-clad guarantee against concurrent duplicate requests that slip through the application check simultaneously.

---

### 🔒 Optimistic Locking with Automatic Retry

Pessimistic locking would serialize all transfers through a queue and destroy throughput. Instead, every `Account` entity carries a `@Version` field that Hibernate auto-increments on each update.

```
Thread A reads Account X  →  version = 0, balance = ₹1,000
Thread B reads Account X  →  version = 0, balance = ₹1,000

Thread A commits  →  UPDATE ... WHERE version = 0  →  ✅  version → 1
Thread B commits  →  UPDATE ... WHERE version = 0  →  ❌  ObjectOptimisticLockingFailureException
                                                          (version is now 1, not 0)

@Retryable catches the exception
  →  Thread B re-reads  →  version = 1, balance = ₹900
  →  Retries up to 3×, 1s backoff
  →  ✅  Commits at version 2, balance = ₹800

Both transfers complete correctly. No balance lost. No serialization.
```

---

### ⛓️ Blockchain-Style Hash Chaining

Every ledger entry stores a SHA-256 hash of its own content, chained to the previous entry's hash. This creates a cryptographic link that makes silent modification of historical records detectable.

```
Entry #1 │ prevHash = "MANK_1008"   ← genesis constant
         │ hash = SHA256(prevHash + amount + referenceId + timestamp)
         │      = "abc123..."

Entry #2 │ prevHash = "abc123..."   ← points to Entry #1
         │ hash = SHA256(prevHash + amount + referenceId + timestamp)
         │      = "def456..."

Entry #3 │ prevHash = "def456..."   ← points to Entry #2
         │ hash = "ghi789..."
```

Modify any historical entry → its hash changes → the next entry's `prevHash` no longer matches → chain breaks. The admin audit endpoint walks the entire chain and pinpoints the exact entry where corruption occurred.

---

### 🛡️ Distributed Rate Limiting

Rate limiting uses the **token bucket algorithm** via Bucket4j, with buckets persisted in Redis rather than application memory. This is the critical design choice: limits hold correctly even when the same user's requests hit different app instances behind the load balancer.

```
General endpoints      →  10 tokens / minute  (greedy — bursts allowed)
Transaction endpoints  →   1 token  / minute  (interval — strictly one per minute)

Redis key: rate_limit:{TYPE}:{USER_ID}

User hits app1  →  consumes 1 token from Redis bucket
User hits app2  →  same Redis bucket  →  0 tokens left  →  HTTP 429

Circumventing the limit by round-robining between servers is impossible.
```

---

### ⚡ Redis Caching with Security

Balance lookups are cached in Redis with a 10-minute TTL. The cache stores the `ownerId` alongside the balance inside an `AccountCacheDTO`, and verifies ownership on every cache hit — preventing a subtle vulnerability where one user's cached balance could be read by a different authenticated user.

```
Cache key:   balance:{accountId}
Cache value: AccountCacheDTO { ownerId: UUID, balance: BigDecimal }

Cache HIT
  ├─ ownerId matches authenticated user  →  return balance immediately  ✅
  └─ ownerId mismatch                    →  HTTP 401 Unauthorized       ❌

Cache MISS     →  query database  →  populate cache  →  return balance
Redis failure  →  log warning     →  query database  →  return balance
                                      (graceful degradation, zero user impact)
```

Cache is invalidated for both sender and receiver accounts on every transfer.

---

### 🔐 JWT Auth via HttpOnly Cookies

JWTs are delivered as `HttpOnly; Secure; SameSite=Strict` cookies, not `Authorization` headers. This design choice means the token is never accessible via JavaScript — XSS attacks cannot steal it even if they execute on the page.

```
Sign in  →  Set-Cookie: jwtCookie=eyJ...; HttpOnly; Secure; SameSite=Strict
            Browser stores and auto-sends the cookie on every subsequent request
            document.cookie cannot read this token from JavaScript

Each request  →  AuthTokenFilter intercepts before controller
              →  Validates JWT signature + expiry
              →  Populates SecurityContext with authenticated identity
              →  Controller runs with guaranteed authentication
```

---

### 👮 Role-Based Access Control

Two roles enforced at method level via `@PreAuthorize`, with an additional ownership check inside every service method.

| Role | Permissions |
|---|---|
| `ROLE_USER` | Create accounts · transfer money · view own statements and balances |
| `ROLE_ADMIN` | All of the above · audit any account's blockchain chain |

Authorization operates at two distinct layers: Spring Security rejects the request before the method body runs, and the service layer explicitly verifies the authenticated user owns the target account — preventing privilege escalation even with a valid token.

---

### 📊 Paginated Account Statements

The statement endpoint exposes ledger history with full pagination metadata, configurable sort field and direction, and a derived transaction type — positive amount = `CREDIT`, negative = `DEBIT`. No raw internal ledger entity structure leaks to the client.

```
GET /api/statement/{accountId}?pageNumber=0&pageSize=20&sortBy=loggedAt&sortOrder=desc
```

---

### ✅ Input Validation and Error Handling

All request DTOs use Bean Validation (`@NotBlank`, `@Size`, `@Positive`, `@Pattern`). A `@RestControllerAdvice` global exception handler maps every exception type — validation failures, auth errors, ownership violations, optimistic lock conflicts, duplicate reference IDs, insufficient funds — to a consistent, structured JSON response. No stack traces, no internal state, no Spring error pages ever reach the client.

---

## 🔐 Security Summary

| Layer | Protection |
|---|---|
| **Transport** | JWT via `HttpOnly; Secure; SameSite=Strict` cookie — JavaScript cannot access the token |
| **Passwords** | BCrypt hashing at strength 10 |
| **Access control** | RBAC via `@PreAuthorize` — method-level enforcement before the method runs |
| **Ownership** | Every API call explicitly verifies the authenticated user owns the target account |
| **Concurrency** | Optimistic locking (`@Version`) prevents race conditions without serializing requests |
| **Idempotency** | Unique `referenceId` with DB `UNIQUE` constraint prevents duplicate transactions |
| **Rate limiting** | Per-user distributed limits via Redis — holds correctly across all app instances |
| **Validation** | Bean Validation on all DTOs, global exception handler for consistent error responses |
| **Session** | Fully stateless — no server-side sessions, no CSRF risk |

---

## 🛡️ Rate Limiting

| Category | Limit | Refill Strategy | Endpoints |
|---|---|---|---|
| **General** | 10 req / min | Greedy (bursts allowed) | Balance, Statement, Account list, Create account |
| **Transaction** | 1 req / min | Interval (strict, no bursts) | Transfer, Deposit, Withdraw |

Limits are stored in Redis and shared across all application instances. A user cannot circumvent the transaction limit by alternating requests between `app1` and `app2`.

---

## 🧪 Testing

Tests are written with **JUnit 5** and **Mockito**, using `@ExtendWith(MockitoExtension.class)` for proper unit isolation. Service dependencies are mocked so tests exercise business logic without touching the database or Redis.

```bash
# Run all tests
./mvnw clean test

# Run a specific test class
./mvnw test -Dtest=AccountServiceImplTest

# Run a specific method
./mvnw test -Dtest=AccountServiceImplTest#transfer_shouldThrow_whenAmountIsZero
```

### Test coverage — 3 files, ~500 lines, 25+ test cases

| Test File | Framework | What is tested |
|---|---|---|
| `AccountServiceImplTest` | JUnit 5 + Mockito | Transfer, Deposit, Balance cache, Statement — 17 test cases |
| `AdminServiceImplTest` | JUnit 5 + Mockito | Blockchain audit: valid chain, broken chain, data modified |
| `AuthEntryPointJwtTest` | JUnit 5 + Mockito | JWT 401 error response format |

### Scenarios covered in `AccountServiceImplTest`

| Area | Scenarios |
|---|---|
| **Transfer** | Zero amount · negative amount · duplicate `referenceId` · account not found · unauthorized sender · insufficient funds · `CENTRAL_BANK` bypass · successful transfer with cache invalidation |
| **Deposit** | Account not found · unauthorized · `CENTRAL_BANK` account missing · successful delegation to transfer |
| **Balance** | Cache hit with owner match · cache hit with owner mismatch (throws 401) · cache miss with DB fallback and cache population · Redis read failure (falls back to DB) · Redis write failure (still returns balance) |
| **Statement** | Account not found · unauthorized access · correct CREDIT/DEBIT classification |

**Techniques used:**
- `@Spy @InjectMocks` — verifies that deposit correctly delegates to the transfer method
- `ArgumentCaptor` — asserts exact values on entities saved to the repository
- `doThrow` on `RedisConnectionFailureException` — tests graceful Redis failure handling

---

## 🤖 CI/CD Pipeline

Runs on every push to `main` / `develop` and on every pull request to `main`:

```
push / PR
    │
    ▼
[Job 1: Unit Tests]  —  ./mvnw clean test  (JDK 17 Temurin)
    │                    Upload test reports to GitHub
    │
    ▼  (only if Job 1 passes)
[Job 2: Build JAR]  —  ./mvnw clean package -DskipTests
    │
    ▼
[Upload Artifact]  —  JAR stored on GitHub Actions for inspection
```

---

## 🐳 Docker Services

Six containers, one command:

| Service | Image | Port | Purpose |
|---|---|---|---|
| `app1` | Local multi-stage build | 8081 | Spring Boot instance 1 |
| `app2` | Local multi-stage build | 8082 | Spring Boot instance 2 |
| `nginx` | `nginx:alpine` | 80 | Round-robin load balancer |
| `postgres` | `postgres:15-alpine` | 5432 | Primary relational database |
| `redis` | `redis:alpine` | 6379 | Cache + distributed rate limiting |
| `pgadmin` | `dpage/pgadmin4` | 5050 | Database inspection UI |

The Dockerfile uses a **multi-stage build**: Maven compiles and packages in a full JDK image, then only the JAR is copied into a slim JRE image. The final image contains no build tooling and is significantly smaller.

---

## 📁 Project Structure

```
src/main/java/com/example/ledgersystem/
├── LedgerSystemApplication.java          # Entry point
├── DataSeeder.java                        # Seeds roles, systemAdmin, CENTRAL_BANK on startup
├── config/
│   └── AppConst.java                      # Pagination constants
├── controller/
│   ├── AccountController.java             # Banking endpoints + rate limit enforcement
│   ├── AuthController.java                # Signup, signin, signout
│   └── AdminController.java              # Audit endpoint (ROLE_ADMIN only)
├── enums/
│   ├── RateLimitType.java                 # GENERAL, TRANSACTION
│   ├── TransactionStatus.java             # PENDING, PROCESSING, COMPLETED, FAILED
│   └── TransactionType.java              # TRANSFER, DEPOSIT, WITHDRAWAL, REVERSAL
├── Exceptions/
│   ├── APIexception.java
│   ├── AccountNotFound.java
│   ├── DuplicateTransactionException.java
│   ├── InsufficientFundsException.java
│   └── MyGlobalExceptionHandler.java      # @RestControllerAdvice
├── model/
│   ├── Account.java                       # @Version for optimistic locking
│   ├── LedgerEntry.java                   # hash + prevHash for blockchain chain
│   ├── Transaction.java                   # Unique referenceId constraint
│   ├── Role.java                          # RBAC role entity
│   └── User.java                          # User with @ManyToMany roles
├── Payloads/
│   ├── AccountCacheDTO.java               # Stored in Redis (balance + ownerId)
│   ├── AccountStatementDTO.java
│   ├── ApiResponse.java
│   ├── Auditresponse.java
│   ├── CreateAccountDTO.java
│   ├── DepositRequestDTO.java
│   ├── MoneyTransferDTO.java
│   ├── StatementResponse.java             # Paginated response wrapper
│   └── WithdrawRequestDTO.java
├── repositories/
│   ├── AccountRepository.java
│   ├── LedgerEntryRepository.java
│   ├── RoleRepository.java
│   ├── TransactionRepository.java
│   └── UserRepository.java
├── Security/
│   ├── Config/
│   │   ├── RedisConfig.java               # Lettuce + Bucket4j ProxyManager
│   │   └── WebSecurityConfig.java         # Filter chain, CORS, session policy
│   ├── jwt/
│   │   ├── AuthEntryPointJwt.java         # 401 response handler
│   │   ├── AuthTokenFilter.java           # JWT extraction on every request
│   │   └── JwtUtils.java                  # Token generation, validation, parsing
│   └── Services/
│       ├── UserDetailsImpl.java           # Spring Security UserDetails wrapper
│       └── UserDetailsServiceImpl.java    # Loads user from DB by username
├── service/
│   ├── AccountService.java                # Interface
│   ├── AccountServiceImpl.java            # Core ledger business logic
│   ├── AdminService.java                  # Interface
│   ├── AdminServiceImpl.java              # Hash chain audit verification
│   └── RateLimitingService.java           # Bucket4j + Redis bucket resolution
└── utils/
    ├── AuthUtils.java                     # SecurityContext helpers
    └── HashUtils.java                     # SHA-256 hashing
```

---

## ⚙️ Configuration

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

| Property | Description | Default |
|---|---|---|
| `spring.datasource.url` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/ledger_db` |
| `spring.data.redis.host` | Redis hostname | `localhost` |
| `spring.data.redis.port` | Redis port | `6379` |
| `spring.app.jwtSecret` | JWT signing key (Base64-encoded) | — |
| `spring.app.jwtExpirationMs` | Token expiry in milliseconds | — |
| `spring.app.jwtCookieName` | Cookie name for JWT storage | — |

---

## 👨‍💻 Author

**Mayank Jain**

- GitHub: [@mayank1008-tech](https://github.com/mayank1008-tech)
- Email: mj.mayank98@gmail.com
- Repository: [CoreLedgerEngine](https://github.com/mayank1008-tech/CoreLedgerEngine)
- LinkedIn: [LinkedIn](https://www.linkedin.com/in/mayank-jain-78a6bb321/)

---

<div align="center">

*Built from scratch. No shortcuts. Every design decision made for a reason.*

</div>
