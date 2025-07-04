# Raffleease API Server

![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)
![Redis](https://img.shields.io/badge/Redis-7.x-red)
![Docker](https://img.shields.io/badge/Docker-blue)
![License](https://img.shields.io/badge/License-Proprietary-lightgrey)

## 📖 Table of Contents

- [Introduction](#introduction)
- [Key Features](#key-features)
- [Technology Stack](#technology-stack)
- [Architecture & Design](#architecture--design)
- [Database Schema](#database-schema)

---

## Introduction

Raffleease is a full-stack web application designed to streamline and modernize the management of raffles organized by animal rescue associations. Originally developed to address the specific needs of a local organization that had been handling its raffles entirely manually, the project aims to resolve common operational and administrative issues through digitalization.

The platform enables the creation and management of raffles, ticket reservations and sales, and provides full lifecycle control of customer orders—from initial creation to finalization. It also includes a robust user management system with role-based access control, allowing associations to assign different levels of responsibility to their members.

What began as a tailored solution for a single association has since evolved into a more flexible and scalable system, capable of supporting multiple organizations with similar goals. This document provides a comprehensive overview of the RESTful API exposed by the Raffleease server.

---

## Key Features

-   🧱 **Multi-Tenant Architecture:** The application is designed to serve multiple organizations within a single instance. Tenancy is enforced by scoping most endpoints under the path /associations/{associationId}, which isolates data access and authorization within each organizational context.
-   🔐 **JWT-Based Authentication with Refresh Tokens:** Authentication is implemented using stateless JWT access tokens. For security and session persistence, refresh tokens are delivered via HTTP-only, same-site, secure cookies, minimizing exposure to XSS attacks.
-   🛡️ **Role-Based Access Control (RBAC):** Authorization is enforced using Spring Security and aspect-oriented programming (AOP). Roles are defined per association, and each secured operation is guarded by role checks performed through custom annotations and interceptors.
-   ♻️ **Centralized Error Handling:** All exceptions and validation errors are intercepted and processed by a global exception handler. Responses are returned in a consistent format that includes an HTTP status code, internal error code, and a structured payload.
-   📦 **Standardized API Response Format:** All endpoints follow a standardized response structure using a response factory pattern, ensuring consistency in success and error cases.
-   🚦 **Rate Limiting:** The API includes route-level and user-level rate limiting, implemented via AOP and backed by Redis for distributed request tracking. When limits are exceeded, the system returns a 429 Too Many Requests response with appropriate headers.
-   🧠 **Domain-Based Modular Architecture:** The backend is organized into bounded domains that encapsulate related logic, entities, and services, which improves scalability and enforces separation of concerns.
-   🔍 **Search with Filtering, Sorting, and Pagination:** Search endpoints implement dynamic query generation using JPA CriteriaBuilder, allowing for flexible filtering, multi-field sorting, and paginated results.
-   📬 **Email Notification Integration:** The system integrates with third-party providers for sending transactional email notifications. The notification module is decoupled from business logic and supports multiple channels.
-   🖼️ **Filesystem-Based Media Handling:** Uploaded media is stored and managed on the filesystem with metadata tracking. The system handles file validation, path organization, and secure URL generation.
-   🧪 **Integration Testing with Testcontainers:** The application uses Testcontainers to run full-stack integration tests against real instances of services like PostgreSQL, ensuring high test fidelity and reproducible environments.
-   🔁 **HTTP Filters and Interceptors:** Custom filters are used to log requests, sanitize headers, and manage token extraction. Interceptors manage audit trails and enrich security contexts.
-   📁 **Configuration via Environment Variables and Docker:** All sensitive or environment-specific settings are managed through environment variables. The application is fully containerized using Docker, with services orchestrated via Docker Compose for local development and deployment.

---

## Technology Stack

-   **Backend**: Java 17, Spring Boot 3
-   **Database**: PostgreSQL
-   **In-Memory Cache**: Redis (for rate limiting and session management)
-   **Containerization**: Docker & Docker Compose
-   **Testing**: JUnit 5, Mockito, Testcontainers

---

## Architecture & Design

The application is built on a modular, domain-driven architecture that emphasizes separation of concerns and scalability.

-   **Domain-Based Modules**: The backend is organized into bounded domains (e.g., Users, Orders, Raffles) that encapsulate related logic, entities, and services.
-   **Standardized API Responses**: A response factory pattern ensures all API responses (both success and error) follow a consistent and predictable structure.
-   **HTTP Filters and Interceptors**: Custom filters and interceptors handle cross-cutting concerns like request logging, audit trails, and security context enrichment.
-   **Configuration Management**: All sensitive or environment-specific settings are managed through environment variables, aligning with Twelve-Factor App principles.

---

## Database Schema

The data model is designed to support the multi-tenant architecture and complex relationships between raffles, users, and orders.

### E/R Diagram

```mermaid
erDiagram
    User {
        Long id PK
        String firstName
        String lastName
        String userName UK
        String email UK
        String password
        boolean isEnabled
        LocalDateTime createdAt
        LocalDateTime updatedAt
        Long phone_number_id FK
    }

    UserPhoneNumber {
        Long id PK
        String phoneNumber
        String countryCode
        LocalDateTime createdAt
        LocalDateTime updatedAt
    }

    Association {
        Long id PK
        String name UK
        String description
        String phoneNumber UK
        String email UK
        LocalDateTime createdAt
        LocalDateTime updatedAt
        Long address_id FK
    }

    Address {
        Long id PK
        String placeId
        String formattedAddress
        Double latitude
        Double longitude
        String city
        String province
        String zipCode
    }

    AssociationMembership {
        Long id PK
        AssociationRole role
        Long user_id FK
        Long association_id FK
    }

    Raffle {
        Long id PK
        String title
        String description
        RaffleStatus status
        BigDecimal ticketPrice
        Long totalTickets
        Long firstTicketNumber
        CompletionReason completionReason
        LocalDateTime createdAt
        LocalDateTime updatedAt
        LocalDateTime startDate
        LocalDateTime endDate
        LocalDateTime completedAt
        Long association_id FK
        Long winning_ticket_id FK
    }

    RaffleStatistics {
        Long id PK
        Long availableTickets
        Long soldTickets
        BigDecimal revenue
        BigDecimal averageOrderValue
        Long totalOrders
        Long completedOrders
        Long pendingOrders
        Long cancelledOrders
        Long unpaidOrders
        Long refundedOrders
        Long participants
        BigDecimal ticketsPerParticipant
        LocalDateTime firstSaleDate
        LocalDateTime lastSaleDate
        BigDecimal dailySalesVelocity
        Long raffle_id FK
    }

    Ticket {
        Long id PK
        String ticketNumber
        TicketStatus status
        LocalDateTime createdAt
        LocalDateTime updatedAt
        Long raffle_id FK
        Long cart_id FK
        Long customer_id FK
    }

    Cart {
        Long id PK
        CartStatus status
        LocalDateTime createdAt
        LocalDateTime updatedAt
        Long user_id FK
    }

    Customer {
        Long id PK
        String fullName
        String email
        LocalDateTime createdAt
        LocalDateTime updatedAt
        Long phone_number_id FK
    }

    CustomersPhoneNumber {
        Long id PK
        String phoneNumber
        String countryCode
        LocalDateTime createdAt
        LocalDateTime updatedAt
    }

    Order {
        Long id PK
        OrderStatus status
        String orderReference UK
        String comment
        LocalDateTime createdAt
        LocalDateTime updatedAt
        LocalDateTime completedAt
        LocalDateTime cancelledAt
        LocalDateTime refundedAt
        LocalDateTime unpaidAt
        Long raffle_id FK
        Long customer_id FK
    }

    OrderItem {
        Long id PK
        String ticketNumber
        BigDecimal priceAtPurchase
        Long ticketId
        Long raffleId
        Long customerId
        Long order_id FK
    }

    Payment {
        Long id PK
        PaymentMethod paymentMethod
        BigDecimal total
        LocalDateTime createdAt
        LocalDateTime updatedAt
        Long order_id FK
    }

    Image {
        Long id PK
        ImageStatus status
        String fileName
        String filePath
        String contentType
        String url UK
        Integer imageOrder
        LocalDateTime createdAt
        LocalDateTime updatedAt
        Long raffle_id FK
        Long user_id FK
        Long association_id FK
    }

    Notification {
        Long id PK
        NotificationType notificationType
        NotificationChannel channel
        LocalDateTime notificationDate
    }

    %% Relationships
    User ||--|| UserPhoneNumber : has
    User ||--o{ AssociationMembership : participates
    User ||--o{ Cart : owns
    User ||--o{ Image : uploads

    Association ||--|| Address : located_at
    Association ||--o{ AssociationMembership : contains
    Association ||--o{ Raffle : organizes
    Association ||--o{ Image : owns

    AssociationMembership }o--|| Association : belongs_to
    AssociationMembership ||--|| User : assigned_to

    Raffle ||--|| RaffleStatistics : has_statistics
    Raffle ||--o{ Ticket : contains
    Raffle ||--o{ Order : receives
    Raffle ||--o{ Image : displays
    Raffle }o--|| Association : organized_by
    Raffle ||--o| Ticket : winning_ticket

    Cart ||--o{ Ticket : holds
    Cart }o--|| User : belongs_to

    Customer ||--|| CustomersPhoneNumber : has
    Customer ||--o{ Ticket : purchases
    Customer ||--o{ Order : places

    Order ||--|| Payment : has_payment
    Order ||--|| Customer : placed_by
    Order }o--|| Raffle : for_raffle
    Order ||--o{ OrderItem : contains

    OrderItem }o--|| Order : part_of

    Ticket }o--|| Raffle : belongs_to
    Ticket }o--o| Cart : in_cart
    Ticket }o--o| Customer : owned_by

    Image }o--o| Raffle : illustrates
    Image }o--o| User : uploaded_by
    Image }o--|| Association : belongs_to
