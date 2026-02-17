# PayFlow --- Distributed FinTech Payment Platform

PayFlow is a distributed payment system built using Spring Boot
microservices, Kafka, Redis, and PostgreSQL, enabling secure
wallet-to-wallet money transfers. It uses the Saga pattern for
distributed transaction consistency and an event-driven architecture for
asynchronous reward and notification processing.

This project demonstrates real-world backend design patterns used in
modern fintech systems like PayPal and Stripe.

------------------------------------------------------------------------

# Architecture Overview

PayFlow follows a Microservices Architecture, where each service:

-   is independently deployable\
-   owns its own database\
-   communicates via REST (synchronous) and Kafka (asynchronous)\
-   ensures scalability, fault isolation, and reliability

Core Flow:

Client\
↓\
API Gateway (JWT auth, rate limiting)\
↓\
Transaction Service (Saga orchestrator)\
↓\
Wallet Service (Hold → Capture → Credit)\
↓\
Kafka Event Published\
├── Reward Service consumes event\
└── Notification Service consumes event

------------------------------------------------------------------------

# Sequence Diagram

``` mermaid
sequenceDiagram

actor Client
participant Gateway as API Gateway
participant Txn as Transaction Service
participant Wallet as Wallet Service
participant Kafka as Kafka Broker
participant Reward as Reward Service
participant Notify as Notification Service
participant TxnDB as Transaction DB
participant WalletDB as Wallet DB
participant RewardDB as Reward DB
participant NotifyDB as Notification DB

Client->>Gateway: POST /transactions (JWT)
Gateway->>Gateway: Validate JWT
Gateway->>Txn: Forward request (X-User-Id)

Txn->>TxnDB: Save Transaction (PENDING)

Txn->>Wallet: Place HOLD (sender funds)
Wallet->>WalletDB: Lock wallet (PESSIMISTIC_WRITE)
Wallet->>WalletDB: Create WalletHold
Wallet-->>Txn: HoldReference

Txn->>Wallet: Capture HOLD
Wallet->>WalletDB: Deduct balance
Wallet-->>Txn: Capture Success

Txn->>Wallet: Credit receiver wallet
Wallet->>WalletDB: Add balance
Wallet-->>Txn: Credit Success

Txn->>TxnDB: Update Transaction (SUCCESS)

Txn->>Kafka: Publish txn-initiated event

Kafka-->>Reward: Consume event
Reward->>RewardDB: Save reward

Kafka-->>Notify: Consume event
Notify->>NotifyDB: Save notification

Txn-->>Gateway: Return SUCCESS
Gateway-->>Client: Transaction Complete
```

------------------------------------------------------------------------

# Microservices

## API Gateway

-   JWT authentication and validation\
-   Request routing to services\
-   Redis-based rate limiting

## User Service

-   User registration and login\
-   JWT token generation\
-   Creates wallet via Wallet Service

## Wallet Service

-   Wallet creation and balance management\
-   Credit, debit, hold, capture, and release operations\
-   Uses pessimistic locking to prevent double spending

## Transaction Service

-   Orchestrates distributed transactions using Saga pattern\
-   Coordinates wallet hold, capture, and credit\
-   Publishes Kafka events after successful transactions

## Reward Service

-   Kafka consumer for transaction events\
-   Calculates and stores reward points\
-   Implements idempotency to prevent duplicate rewards

## Notification Service

-   Kafka consumer for transaction events\
-   Generates and stores user notifications\
-   Fully asynchronous and decoupled

------------------------------------------------------------------------

# Technology Stack

Backend: Java 17, Spring Boot, Spring Security, Spring Cloud Gateway\
Database: PostgreSQL\
Messaging: Apache Kafka\
Caching: Redis\
Containerization: Docker, Docker Compose

------------------------------------------------------------------------

# Getting Started

Prerequisites:

-   Java 17+
-   Docker
-   Docker Compose
-   Maven

Clone:

git clone https://github.com/anirban-rudra/PayFlow.git\
cd PayFlow

Start infrastructure:

docker-compose up -d

Start services:

cd user-service\
./mvnw spring-boot:run

Repeat for other services.
wallet-service
transaction-service
reward-service
notification-service
api-gateway

Access API Gateway

------------------------------------------------------------------------

# Author

Anirban Rudra\
Software Engineer - Java Backend\
SAP Fioneer

LinkedIn: https://www.linkedin.com/in/anirban-rudra45/
