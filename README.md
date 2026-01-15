# Evolyn-Data-Service

Evolyn-Data-Service is a Spring Boot–based backend microservice that acts as the **core data persistence layer** for the Evolyn platform. It receives internal requests from **Evolyn-API** and stores transactional data in **PostgreSQL** while managing logs, events, and audit data in **MongoDB**.

This service is designed for scalability, clean separation of concerns, and secure internal communication within a microservices architecture.

---

## High-Level Architecture

```
Frontend
   ↓
Evolyn-API (Authentication, Authorization, Gateway)
   ↓
Evolyn-Data-Service
   ├── PostgreSQL (Transactional / Relational Data)
   └── MongoDB (Logs / Events / Audit Data)
```

---

## Tech Stack

* Java 17+
* Spring Boot 4.x
* Spring Web
* Spring Data JPA (PostgreSQL)
* Spring Data MongoDB
* PostgreSQL
* MongoDB
* Maven

---

## Responsibilities

* Persist core transactional data in PostgreSQL
* Store logs, events, and audit trails in MongoDB
* Expose internal REST APIs for Evolyn-API
* Enforce clean separation between relational and document data
* Act as the system of record for the Evolyn platform

---

## Project Structure

```
src/main/java/com/evolyn/dataservice
 ├── controller        # Internal REST controllers
 ├── service           # Business logic
 ├── postgres
 │   ├── entity        # JPA entities
 │   └── repository    # JPA repositories
 ├── mongo
 │   ├── document      # MongoDB documents
 │   └── repository    # Mongo repositories
 └── config            # Configuration classes
```

---

## Configuration

Create or update the following file:

`src/main/resources/application.properties`

```properties
# Application
spring.application.name=Evolyn Data Service
server.port=8081

# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/evolyn_db
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect

# MongoDB
spring.data.mongodb.uri=mongodb://localhost:27017/evolyn_db
```

---

## ▶️ Running the Application

### Prerequisites

* PostgreSQL running
* MongoDB running
* Java 17+
* Maven

### Run using Maven

```bash
mvn spring-boot:run
```

### Run as JAR

```bash
mvn clean package
java -jar target/evolyn-data-service-0.0.1-SNAPSHOT.jar
```

The service will start on:

```
http://localhost:8081
```

---

## API Usage

This service is **not exposed to the public**.

It is intended to be accessed **only by Evolyn-API** through internal endpoints such as:

```
POST /internal/**
```

---

## Security Notes

* This service assumes requests are pre-authenticated by Evolyn-API
* Future enhancements may include:

  * JWT validation
  * mTLS or API key–based service authentication

---

## Design Decisions

* PostgreSQL is used for transactional consistency and relational integrity
* MongoDB is used for flexible, schema-less data such as logs and events
* Entities and documents are strictly separated
* No direct frontend access is allowed

---

##  Maintainer

**Evolyn Platform Team**

---

## License

This project is proprietary and intended for internal use within the Evolyn platform.
