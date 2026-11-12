# 🚆 RailBook – Train Ticket Booking System

A backend-driven train ticket booking application built using **Java 21, Spring Boot, Spring Data JPA, and PostgreSQL**, designed to handle seat reservations safely through transactional operations, temporary seat holds, and pessimistic database locking.

## 📌 Overview

RailBook is a train ticket booking system that provides REST APIs for searching trains, reserving seats, confirming bookings, cancelling reservations, and managing booking history.

The application focuses on maintaining data consistency and preventing duplicate seat reservations, even when multiple users attempt to book the same seat concurrently.

## ✨ Key Features

- **Train Search:** Search available trains and retrieve journey-related information.
- **Seat Reservation:** Reserve individual seats for a selected journey.
- **Temporary Seat Holds:** Hold selected seats for 5 minutes before booking confirmation.
- **Concurrency Control:** Implemented pessimistic locking to prevent multiple users from reserving the same seat simultaneously.
- **Automatic Hold Expiry:** Expired seat holds are released, making seats available for other users.
- **Booking Confirmation:** Confirm reservations using validated hold tokens.
- **Booking Cancellation:** Cancel confirmed bookings and update seat inventory accordingly.
- **Booking History:** Retrieve booking records and reservation details.
- **Transactional Operations:** Maintain consistency across booking, cancellation, and seat inventory updates.

## 🛠️ Tech Stack

| Technology | Purpose |
|---|---|
| Java 21 | Backend programming |
| Spring Boot | Application development |
| Spring MVC | REST API development |
| Spring Data JPA | Database operations and ORM |
| PostgreSQL | Relational database |
| Maven | Dependency management and build automation |
| Postman | API testing |
| Git & GitHub | Version control |

## 🏗️ System Design

RailBook follows a layered backend architecture to separate business logic, API handling, and database operations.

```text
              Client / Postman
                     |
                     v
              REST Controllers
                     |
                     v
                Service Layer
                     |
                     v
              Repository Layer
                     |
                     v
                PostgreSQL
```

**Architecture Components:**

- **Controller Layer:** Handles HTTP requests and API responses.
- **Service Layer:** Implements booking logic, validation, and transactional workflows.
- **Repository Layer:** Handles database operations using Spring Data JPA.
- **Entity Layer:** Represents relational database entities and their relationships.

## 🔒 Concurrency Control & Double Booking Prevention

One of the main challenges in a train booking system is preventing two users from booking the same seat simultaneously.

RailBook addresses this using **Pessimistic Write Locking** and temporary seat reservations.

### Booking Workflow

1. A user requests a seat reservation.
2. The application acquires a pessimistic write lock on the corresponding seat inventory record.
3. The system checks whether the seat is available or already booked.
4. If available, the seat is marked as `HELD`, and a unique hold token is generated.
5. The seat remains reserved for 5 minutes while the user completes the booking process.
6. Another user attempting to reserve the same seat must wait for the lock and cannot acquire an already-held seat.
7. Once the booking is confirmed, the seat status changes to `BOOKED`.
8. If the hold expires, the seat becomes available for another reservation.

### Seat Lifecycle

```text
       AVAILABLE
            |
            v
          HELD
         /    \
        v      v
     BOOKED  EXPIRED
                |
                v
            AVAILABLE
```

This mechanism ensures that concurrent booking requests cannot successfully reserve the same seat.

## 🗄️ Database Design

The application uses PostgreSQL with Spring Data JPA for relational data management.

Key entities include:

- **Train:** Stores train information.
- **Trip:** Represents a train journey on a particular date.
- **Seat:** Stores coach and seat details.
- **SeatInventory:** Tracks the availability and reservation status of seats.
- **Booking:** Maintains confirmed reservation details.

Database constraints and indexes help maintain data integrity and support efficient queries.

## 🧪 Testing

The application has been validated through **32 automated tests** covering important backend workflows.

Testing includes:

- Seat reservation and availability validation.
- Concurrent seat reservation handling.
- Temporary hold expiration.
- Booking confirmation and hold-token validation.
- Booking cancellation.
- Seat inventory consistency.
- Authentication-related functionality.

## ⚙️ Getting Started

### Prerequisites

Make sure you have the following installed:

- Java 21 or later
- Maven 3.9+
- PostgreSQL
- Git

### 1. Clone the Repository

```bash
git clone https://github.com/Krishna2006-babu/RailBook.git
```

### 2. Navigate to the Project

```bash
cd RailBook
```

### 3. Configure PostgreSQL

Create a PostgreSQL database and configure the database connection in your Spring Boot application properties.

Set your database URL, username, and password according to your local PostgreSQL configuration.

### 4. Run the Application

```bash
mvn spring-boot:run
```

The Spring Boot application will start on the configured server port.

### 5. Run Tests

```bash
mvn test
```

## 🚀 Future Enhancements

- Frontend integration for a complete booking experience.
- Payment gateway integration.
- PNR generation and booking status tracking.
- Deployment with production-ready infrastructure.
- Enhanced authentication and authorization.
- Advanced concurrency and load testing.

## 👨‍💻 Author

**Krishna Sharma**

Electronics and Communication Engineering  
National Institute of Technology, Silchar

- [GitHub](https://github.com/Krishna2006-babu)
- [LinkedIn](https://www.linkedin.com/in/krishna-sharma-80a10b21a/)
- [LeetCode](https://leetcode.com/u/MEOWMEOW06/)

---

⭐ If you find this project interesting, consider giving the repository a star!
