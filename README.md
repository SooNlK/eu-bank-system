# EU Bank System

Celem projektu jest implementacja procesu biznesowego: systemu rozliczania przelewów i płatności pomiędzy bankami.

## Stack

| Warstwa         | Technologia                                                   |
|-----------------|---------------------------------------------------------------|
| Backend         | Java 17, Spring Boot, Spring Data JPA, Spring Security, Maven |
| Frontend        | React 19, Vite                                                |
| Baza danych     | PostgreSQL 16, system migracji: Flyway                        |
| Infrastruktura  | Docker, Docker Compose                                        |

## Zakres Funkcjonalności


## Uruchomienie aplikacji

Aby uruchomić aplikację w środowisku developerskim, upewnij się, że posiadasz zainstalowane narzędzia Docker oraz Docker Compose.

1. Sklonuj repozytorium:
   ```bash
    git clone https://github.com/SooNlK/eu-bank-system.git
    cd eu-bank-system
   ```

2. Skonfiguruj zmienne środowiskowe:
    ```bash
    cp .env.example .env
    ```
    *Ewentualnie dostosuj wartości takie jak hasła czy loginy w pliku `.env`.*

4. Uruchom kontenery przy użyciu dokera:
    ```bash
    docker-compose up --build
    ```
4. Aplikacja powinna być dostępna:
    - Frontend pod adresem: `http://localhost:<PORT_Z_ENV>` (domyślnie: np. 80 / 3000 w zależności od vity/nginx)
    - Backend pod adresem: `http://localhost:<PORT_Z_ENV>` (domyślnie: 8080)
    - Baza Danych (Port mapowany: `5432`)
