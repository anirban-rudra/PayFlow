# PayFlow — Distributed FinTech Payment System

PayFlow is a PayPal-like distributed payment platform built using Spring Boot microservices architecture. It supports secure wallet-to-wallet transfers, asynchronous transaction processing, rewards, and notifications using an event-driven architecture.

This project demonstrates production-grade backend design patterns including distributed transactions, event-driven communication, fault isolation, and scalable microservices.

---

## Architecture Overview

The system follows a Microservices Architecture where each service is independently deployable and scalable.

Core services:

- API Gateway — routing, authentication, rate limiting
- User Service — user registration and authentication
- Wallet Service — balance management and ledger logic
- Transaction Service — peer-to-peer payments and transaction processing
- Reward Service — cashback and rewards processing
- Notification Service — event-driven user notifications

Communication patterns:

- REST APIs for synchronous communication
- Kafka for asynchronous event-driven communication

Each service maintains its own database for loose coupling and scalability.

---

## Architecture Diagram

![Architecture](docs/architecture.png)

---

## Key Features

### Distributed Transaction Handling
Implements distributed transaction coordination between Wallet and Transaction services to ensure balance consistency.

### Event-Driven Architecture
Kafka is used to publish transaction events consumed by Reward and Notification services asynchronously.

### Wallet Ledger Management
Wallet Service manages:

- Available balance
- Debit and credit operations
- Transaction history
- Hold and capture operations

### Fault Isolation
Service failures (e.g., notification failure) do not affect payment processing.

### Secure Authentication
JWT-based stateless authentication using Spring Security.

### API Gateway
Central entry point providing:

- Request routing
- Authentication validation
- Rate limiting using Redis

---

## Tech Stack

Backend:
- Java
- Spring Boot
- Spring Security
- Spring Cloud Gateway

Messaging:
- Apache Kafka

Caching and Rate Limiting:
- Redis

Database:
- PostgreSQL / MySQL

Infrastructure:
- Docker
- Docker Compose

Build Tool:
- Maven

---

## Microservices Overview

User Service:
- User registration and authentication
- JWT token generation

Wallet Service:
- Wallet creation and balance management
- Debit, credit, hold, and release operations

Transaction Service:
- Handles peer-to-peer money transfers
- Publishes transaction events to Kafka

Reward Service:
- Consumes Kafka events
- Issues cashback rewards

Notification Service:
- Consumes Kafka events
- Sends user notifications

API Gateway:
- Routes incoming requests
- Enforces authentication and rate limiting

---

## Running the Project

Start infrastructure:

