# Hamro Chalchitraghar Backend

Spring Boot backend for Hamro Chalchitraghar, a cinema ticket booking API with movie, hall, show, seat, user, and booking management.

## Documentation

- [Product Requirements](docs/PRD.md)
- [Technical Requirements](docs/TRD.md)
- [Architecture](docs/ARCHITECTURE.md)
- [API Reference](docs/API.md)
- [Setup Guide](docs/SETUP.md)
- [Database Documentation](docs/DATABASE.md)
- [Maintenance Guide](docs/MAINTENANCE.md)

## Quick Start

Prerequisites: Java 21 and MySQL.

Create the local database:

```sql
CREATE DATABASE hamro_chalachitraghar_db;
```

Run the application on Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Health check:

```text
GET http://localhost:8080/api/health
```

See [Setup Guide](docs/SETUP.md) for full local setup and troubleshooting.
