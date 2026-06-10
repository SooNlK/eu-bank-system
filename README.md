# EU Bank System

Projekt systemu bankowego w strefie euro obsługującego konta osobiste, konta dla dzieci (Junior) z mechanizmem zatwierdzania przelewów, transakcje kartowe, a także rozliczenia przelewów krajowych i międzynarodowych w formatach ISO 20022 / SWIFT.

## Stack Technologiczny

| Warstwa | Technologia                                                         |
| :--- |:--------------------------------------------------------------------|
| **Backend** | Java 17 + Spring Boot 3 + Spring Data JPA + Spring Security + Maven |
| **Frontend** | React 19 + Vite                                                     |
| **Baza danych** | PostgreSQL 16 + Flyway (migracje schematów od V1 do V19)            |
| **Infrastruktura** | Docker + Docker Compose                                             |

---

## Zakres Funkcjonalności

### 1. Konta STANDARD oraz JUNIOR (Rodzic-Dziecko)
System wspiera dwa rodzaje rachunków w klasie [AccountType](file:///Users/krzysztof/Desktop/eu-bank-system/backend/src/main/java/com/bank/domain/account/AccountType.java):
*   **STANDARD**: Klasyczne konto bankowe dla dorosłego klienta.
*   **JUNIOR**: Konto dedykowane dla dzieci w wieku 7-13 lat. Zakładane jest wyłącznie przez zalogowanego rodzica za pośrednictwem endpointu w [AccountController.java](file:///Users/krzysztof/Desktop/eu-bank-system/backend/src/main/java/com/bank/api/AccountController.java).

#### Przepływ zatwierdzania (Parent Approval Flow):
Wszystkie przelewy zlecane z konta typu **JUNIOR** (wewnętrzne oraz zewnętrzne) wymagają zgody rodzica lub uprawnionego opiekuna.
*   Przelew taki trafia do bazy ze statusem `PENDING_APPROVAL` oraz `requiresApproval = true`.
*   Rodzic widzi oczekujące przelewy na dedykowanym panelu Juniora (endpoint `GET /api/transfers/pending-approval`).
*   Zatwierdzenie (`POST /api/transfers/{id}/approve`) lub odrzucenie (`POST /api/transfers/{id}/reject`) realizowane jest w [TransferService.java](file:///Users/krzysztof/Desktop/eu-bank-system/backend/src/main/java/com/bank/service/TransferService.java) i natychmiastowo procesuje lub anuluje środki.

---

### 2. Przelew Wewnętrzny i Zewnętrzny (SEPA & TARGET2)
Obsługiwane są kanały płatności zdefiniowane w [TransferChannel](file:///Users/krzysztof/Desktop/eu-bank-system/backend/src/main/java/com/bank/domain/transfer/TransferChannel.java):
*   `INTERNAL`: Szybki przelew bezprowizyjny pomiędzy rachunkami w tym samym banku.
*   `SEPA` (SEPA Credit Transfer): Przelew zewnętrzny rozliczany sesyjnie (Batch). Transakcje są wysyłane jako komunikaty ISO 20022 XML do klienta sesyjnego i oznaczane jako `COMPLETED` po rozliczeniu sesji.
*   `SEPA_INSTANT`: Przelew natychmiastowy do limitu 100 000 EUR na pojedynczą transakcję.
*   `TARGET` (TARGET2): Rozliczenia brutto w czasie rzeczywistym (RTGS) dla dużych kwot w strefie euro, wymagające podania kodu BIC banku odbiorcy.

#### Integracja Webhooków SEPA/TARGET:
Backend udostępnia kontroler [TargetWebhookController.java](file:///Users/krzysztof/Desktop/eu-bank-system/backend/src/main/java/com/bank/api/TargetWebhookController.java) (`POST /api/v1/target-settlement`) do przyjmowania powiadomień rozliczeniowych i uznawania rachunków dla przelewów przychodzących z symulatora płatniczego.

---

### 3. Integracja z Siecią SWIFT (Przelew Międzynarodowy)
Obsługuje transgraniczne i wielowalutowe przelewy międzynarodowe w kanale `SWIFT`. Integracja łączy się z zewnętrznym symulatorem sieci SWIFT (`Jkwasnyy/SWIFT-Aplikacje-Biznesowe`) pod adresem `SWIFT_URL`.

#### Kluczowe elementy logiki SWIFT:
1.  **Format wiadomości**: Wymiana danych odbywa się za pomocą standardowych komunikatów XML **pacs.008** (FIToFICustomerCreditTransfer) generowanych przez [Pacs008Builder.java](file:///Users/krzysztof/Desktop/eu-bank-system/backend/src/main/java/com/bank/client/swift/Pacs008Builder.java) oraz parsowanych przez [Pacs008Parser.java](file:///Users/krzysztof/Desktop/eu-bank-system/backend/src/main/java/com/bank/client/swift/Pacs008Parser.java).
2.  **Obsługa walut i FX**: Wspierane waluty docelowe to: `EUR`, `USD`, `GBP`, `PLN`, `CHF`. Konwersje walutowe są realizowane na bazie kursów zapisanych w tabeli `FX_RATES` poprzez [FxService.java](file:///Users/krzysztof/Desktop/eu-bank-system/backend/src/main/java/com/bank/service/FxService.java).
3.  **Opłaty SWIFT**: Za przelew pobierana jest prowizja zdefiniowana w konfiguracji (domyślnie 1% kwoty przelewu, pobierane z konta nadawcy przy podziale kosztów `SHAR` lub `DEBT`).
4.  **Rachunki korespondenckie (Nostro)**: Wypływ środków w obcej walucie jest automatycznie rozliczany na odpowiednim koncie Nostro naszego banku (tabela `CORRESPONDENT_ACCOUNTS`) u pierwszego korespondenta na trasie płatności.
5.  **Obsługa wiadomości przychodzących**: [SwiftWebhookController.java](file:///Users/krzysztof/Desktop/eu-bank-system/backend/src/main/java/com/bank/api/SwiftWebhookController.java) (`/receive` oraz `/api/v1/swift/receive`) odbiera przelewy przychodzące:
    *   Jeśli rachunek odbiorcy jest aktywny, środki są przeliczane i księgowane (status `ACCEPTED`).
    *   Jeśli konto jest zamknięte lub nie istnieje, wysyłany jest automatyczny zwrot (**Recall**) do banku nadawcy, a żądanie kończy się statusem `REJECTED`.

---

### 4. Integracja z Modułem Kart Płatniczych
Backend banku integruje się z zewnętrzną siecią kartową (`FilipSl3/Karty-Platnicze-Aplikacje-Biznesowe`) jako bank-wydawca (Issuer).

*   Komunikacja REST z gatewayem sieci kart pod adresem zdefiniowanym w `CARD_NETWORK_BASE_URL`.
*   Bank zapisuje lokalnie wyłącznie bezpieczne, zamaskowane dane karty (token zewnętrzny, status, 4 ostatnie cyfry, datę ważności, limity dzienne/miesięczne). Pełny PAN i CVV są zwracane tylko raz w odpowiedzi na endpoint wydania karty.
*   Udostępniane są callbacki autoryzacyjne w [CardIssuerController.java](file:///Users/krzysztof/Desktop/eu-bank-system/backend/src/main/java/com/bank/api/CardIssuerController.java):
    *   `POST /api/v1/authorize` – blokada środków na koncie w odpowiedzi na próbę płatności kartą.
    *   `POST /api/v1/capture` – ostateczne rozliczenie i ściągnięcie zablokowanych środków.
    *   `POST /api/v1/refund` – zwrot transakcji na rachunek karty.

---

### 5. Płatności BLIK (API)
System posiada zdefiniowaną strukturę danych oraz endpointy w [BlikController.java](file:///Users/krzysztof/Desktop/eu-bank-system/backend/src/main/java/com/bank/api/BlikController.java) do generowania 6-cyfrowych kodów BLIK ważnych przez 120 sekund.

---

## Struktura Bazy Danych

Poniższy diagram ERD ilustruje tabele bazy danych oraz relacje między nimi (stan zgodny z migracją Flyway V19):

```mermaid
erDiagram
    CUSTOMERS {
        UUID id PK
        VARCHAR email "UNIQUE"
        VARCHAR password_hash
        VARCHAR first_name
        VARCHAR last_name
        VARCHAR passport_number
        DATE date_of_birth
        VARCHAR status
        TIMESTAMP created_at
    }

    ACCOUNTS {
        UUID id PK
        UUID customer_id FK
        UUID parent_account_id FK
        VARCHAR account_number "UNIQUE"
        VARCHAR type "STANDARD, JUNIOR"
        DECIMAL balance
        DECIMAL reserved_balance
        VARCHAR currency
        VARCHAR status
        TIMESTAMP created_at
    }

    TRANSACTIONS {
        UUID id PK
        UUID account_id FK
        UUID card_id FK
        DECIMAL amount
        VARCHAR currency
        VARCHAR type "DEBIT, CREDIT"
        VARCHAR status
        VARCHAR description
        VARCHAR reference_id
        VARCHAR counterparty_name
        VARCHAR counterparty_iban
        TIMESTAMP created_at
    }

    TRANSFERS {
        UUID id PK
        UUID from_account_id FK
        UUID to_account_id FK
        DECIMAL amount
        VARCHAR currency
        VARCHAR channel "INTERNAL, SEPA, SEPA_INSTANT, TARGET, SWIFT"
        VARCHAR status
        VARCHAR external_reference_id
        VARCHAR description
        DATE value_date
        TIMESTAMP created_at
        TIMESTAMP completed_at
        BOOLEAN requires_approval
        UUID approved_by FK
        TIMESTAMP approved_at
        TIMESTAMP rejected_at
        VARCHAR swift_msg_id
        VARCHAR swift_uetr
        VARCHAR swift_charge_bearer
        VARCHAR swift_route
        DECIMAL swift_fee
        DECIMAL swift_fx_rate
        VARCHAR swift_target_currency
        BOOLEAN swift_recalled
    }

    CARDS {
        UUID id PK
        UUID account_id FK
        VARCHAR last4
        VARCHAR type
        VARCHAR status
        VARCHAR external_card_token
        DATE expires_at
        TIMESTAMP created_at
        DECIMAL daily_limit
        DECIMAL monthly_limit
        VARCHAR currency
    }

    BLIK_CODES {
        UUID id PK
        UUID account_id FK
        VARCHAR code_hash
        BOOLEAN used
        TIMESTAMP expires_at
        TIMESTAMP created_at
    }

    CORRESPONDENT_ACCOUNTS {
        UUID id PK
        VARCHAR correspondent_bic
        VARCHAR correspondent_name
        VARCHAR account_number "UNIQUE"
        VARCHAR currency
        DECIMAL balance
        VARCHAR status
        TIMESTAMP created_at
    }

    FX_RATES {
        UUID id PK
        VARCHAR from_currency
        VARCHAR to_currency
        DECIMAL rate
        TIMESTAMP effective_at
    }

    %% RELACJE
    CUSTOMERS ||--o{ ACCOUNTS : owns
    ACCOUNTS ||--o{ TRANSACTIONS : has
    ACCOUNTS ||--o{ CARDS : has
    ACCOUNTS ||--o{ BLIK_CODES : generates
    ACCOUNTS ||--o{ TRANSFERS : "from"
    ACCOUNTS ||--o{ TRANSFERS : "to"
    ACCOUNTS ||--o{ ACCOUNTS : parent_child
    CARDS ||--o{ TRANSACTIONS : associates
    CUSTOMERS ||--o{ TRANSFERS : approves
```

---

## Konfiguracja Środowiskowa (`.env`)

Przed uruchomieniem aplikacji utwórz plik `.env` na podstawie [.env.example](file:///Users/krzysztof/Desktop/eu-bank-system/.env.example):
```bash
cp .env.example .env
```

Główne parametry konfiguracyjne:

| Zmienna | Domyślna Wartość                   | Opis |
| :--- |:-----------------------------------| :--- |
| **POSTGRES_DB** | `bankdb`                           | Nazwa bazy danych PostgreSQL. |
| **POSTGRES_USER** | `admin`                            | Użytkownik bazy danych. |
| **POSTGRES_PASSWORD** | `secret`                           | Hasło użytkownika bazy danych. |
| **DB_HOST_PORT** | `5433`                             | Port PostgreSQL na maszynie hosta. |
| **BACKEND_PORT** | `8090`                             | Port, na którym nasłuchuje API backendu. |
| **FRONTEND_PORT** | `3010`                             | Port, na którym dostępna jest aplikacja webowa. |
| **CARD_NETWORK_BASE_URL** | `http://host.docker.internal:8072` | Adres URL zewnętrznej sieci kartowej. |
| **CARD_NETWORK_API_KEY** | `bank-key-eu-a`                    | Klucz API do autoryzacji w sieci kartowej. |
| **CARD_NETWORK_HMAC_SECRET**| `secret-eu-a-hmac`                 | Klucz HMAC do weryfikacji podpisów żądań. |
| **TARGET_URL** | `http://host.docker.internal:8001` | Adres URL symulatora TARGET2. |
| **SEPA_BATCH_URL** | `http://host.docker.internal:8002` | Adres URL symulatora SEPA Batch. |
| **SEPA_INSTANT_URL** | `http://host.docker.internal:8003` | Adres URL symulatora SEPA Instant. |
| **SWIFT_URL** | `http://host.docker.internal:3000` | Adres URL zewnętrznego symulatora SWIFT. |
| **SWIFT_CLIENT_ID** | `test-client`                      | Identyfikator klienta do autoryzacji SWIFT. |
| **SWIFT_CLIENT_SECRET** | `test-secret`                      | Sekret autoryzacyjny do symulatora SWIFT. |
| **SWIFT_BANK_BIC** | `BANKDEXX`                         | Kod BIC naszego banku w sieci SWIFT. |
| **SWIFT_ENABLED** | `true`                             | Flaga włączająca/wyłączająca moduł SWIFT. |
| **SWIFT_FEE_PERCENT** | `0.01`                             | Procent pobieranej prowizji SWIFT (0.01 = 1%). |


---

## Uruchomienie Aplikacji i Ekosystemu Płatności

Aby w pełni przetestować wszystkie funkcjonalności, należy uruchomić systemy w następującej kolejności:

### Krok 1: Uruchomienie Infrastruktury Płatności (SEPA/TARGET)
Przelewy krajowe i strefy Euro są rozliczane za pośrednictwem symulatora.
1.  Przejdź do katalogu projektu symulatora płatności (np. `eu-payments-units`):
    ```bash
    cd ../eu-payments-units
    ```
2.  Uruchom kontenery:
    ```bash
    docker compose up -d --build
    ```

### Krok 2: Uruchomienie Symulatora SWIFT
Do testów przelewów zagranicznych SWIFT wymagane jest uruchomienie symulatora SWIFT:
1.  Przejdź do katalogu symulatora SWIFT (np. `swift-simulator`):
    ```bash
    cd ../swift-simulator
    ```
2.  Uruchom usługę na porcie wskazanym w zmiennej `SWIFT_URL`.

### Krok 3: Uruchomienie Banku
1.  Przejdź do katalogu głównego projektu `eu-bank-system`:
    ```bash
    cd ../eu-bank-system
    ```
2.  Uruchom kontenery za pomocą Docker Compose:
    ```bash
    docker compose up -d --build
    ```
3.  Aplikacja bankowa jest dostępna pod adresami:
    *   **Panel Klienta (Frontend)**: [http://localhost:3010](http://localhost:3000) (lub port ustawiony w `FRONTEND_PORT`)
    *   **API (Backend)**: [http://localhost:8090](http://localhost:8080)
    *   **Baza Danych**: Port `5433` (lub port ustawiony w `DB_HOST_PORT`)
    *   **Swagger UI**: [http://localhost:8090/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## Domyślne Dane Testowe (Database Seeding)

Przy pierwszym uruchomieniu baza danych jest automatycznie zasilana przykładowymi danymi przez [DataSeeder.java](file:///Users/krzysztof/Desktop/eu-bank-system/backend/src/main/java/com/bank/bootstrap/DataSeeder.java). Możesz zalogować się poniższymi danymi:

*   **Użytkownik 1 (Rodzic)**:
    *   **Email**: `hans.mueller@example.de`
    *   **Hasło**: `password123`
    *   **Rachunek**: `DE89370400440532013000` (początkowe saldo: `2500.00 EUR`)
*   **Użytkownik 2**:
    *   **Email**: `erika.schmidt@example.de`
    *   **Hasło**: `password123`
    *   **Rachunek**: `DE12500105170648489890` (początkowe saldo: `3200.50 EUR`)

Z poziomu zalogowanego użytkownika Hansa lub Eriki możesz zarejestrować subkonto typu **Junior**, zdefiniować limity na karty oraz przetestować pełen cykl przelewów wewnętrznych, SEPA, TARGET2 oraz SWIFT.
