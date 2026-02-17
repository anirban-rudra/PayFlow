# PayFlow - Distributed FinTech Payment Platform

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

-   is independently deployable
-   owns its own database
-   communicates via REST (synchronous) and Kafka (asynchronous)
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

- JWT authentication and validation
- Request routing to microservices
- Redis-based rate limiting to prevent abuse
- Stateless security enforcement using Spring Cloud Gateway

## User Service

- Handles user registration and login
- Generates JWT tokens for authentication
- Creates wallet automatically via Wallet Service
- Ensures secure user identity management

## Wallet Service

- Manages wallet creation and balance lifecycle
- Supports credit, debit, hold, capture, and release operations
- Uses pessimistic locking to prevent race conditions and double spending
- Ensures ACID-compliant financial transactions

## Transaction Service

- Acts as Saga orchestrator for distributed transactions
- Coordinates wallet hold, capture, and credit operations
- Maintains transaction lifecycle (PENDING → SUCCESS / FAILED)
- Publishes Kafka events after successful transactions

## Reward Service

- Kafka consumer for transaction events
- Calculates and stores reward points
- Implements idempotency to prevent duplicate rewards
- Fully asynchronous and scalable

## Notification Service

- Kafka consumer for transaction events
- Generates and stores user notifications
- Fully asynchronous and decoupled from core payment flow
- Failures do not impact financial transactions


------------------------------------------------------------------------

# Distributed Transaction Flow (Saga)

- Client sends transaction request  
- API Gateway validates JWT and forwards request  
- Transaction Service creates transaction with **PENDING** state  
- Wallet Service places **HOLD** on sender funds  
- Wallet Service **CAPTURES** hold (debits sender)  
- Wallet Service **CREDITS** receiver wallet  
- Transaction Service updates status to **SUCCESS**  
- Kafka event is published  
- Reward Service consumes event and creates reward  
- Notification Service consumes event and creates notification  


------------------------------------------------------------------------

# Engineering Highlights

- Distributed transaction management using Saga pattern  
- Event-driven architecture using Kafka  
- Stateless authentication using JWT  
- Idempotent event processing to prevent duplicate execution  
- Concurrency control using pessimistic locking  
- Independent databases per service for fault isolation and scalability  


------------------------------------------------------------------------
# Technology Stack

## Backend

- Java 17  
- Spring Boot  
- Spring Security  
- Spring Cloud Gateway  

## Database

- PostgreSQL  

## Messaging

- Apache Kafka  

## Caching & Rate Limiting

- Redis  

## Containerization

- Docker  
- Docker Compose  

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
wallet-service\
transaction-service\
reward-service\
notification-service\
api-gateway

Access API Gateway

------------------------------------------------------------------------

# Author

Anirban Rudra\
Software Engineer - Java Backend\
SAP Fioneer

LinkedIn: https://www.linkedin.com/in/anirban-rudra45/
