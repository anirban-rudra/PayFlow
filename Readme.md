# PayFlow - Distributed FinTech Payment System

PayFlow is a PayPal-like distributed payment platform built using Spring Boot microservices architecture. It supports secure wallet-to-wallet transfers, asynchronous transaction processing, rewards, and notifications using an event-driven architecture.

This project demonstrates production-grade backend design patterns including distributed transactions, event-driven communication, fault isolation, rate limiting, load balancing, and scalable microservices.

---

# Architecture Overview

The system follows a Microservices Architecture where each service is independently deployable and scalable.

Core services:

- API Gateway - routing, authentication, rate limiting, and load balancing  
- User Service - user registration and authentication  
- Wallet Service - balance management and ledger operations  
- Transaction Service - peer-to-peer payments and transaction orchestration  
- Reward Service - cashback and reward processing  
- Notification Service - asynchronous event-driven notifications  

Communication patterns:

- REST APIs - synchronous communication between services  
- Kafka - asynchronous event-driven communication  

Each service maintains its own database to ensure loose coupling, scalability, and fault isolation.

---

# Architecture Diagram

![Architecture](docs/architecture.png)

---

## Core Microservices

### User Service
**Responsibilities:** Handles user registration, authentication, and identity management.

**Key Features:**

- Stateless authentication using JWT and Spring Security  
- Secure user registration and login APIs  
- Integrates with Wallet Service to create wallets for new users  

---

### Wallet Service (Core Financial Service)
**Responsibilities:** Manages wallet balances and ensures transactional integrity.

**Key Features:**

- Maintains wallet ledger with available and reserved balances  
- Supports debit, credit, hold, and release operations  
- Prevents overdraft using balance validation and hold mechanisms  
- Ensures ACID-compliant balance updates using transactional persistence  

---

### Transaction Service
**Responsibilities:** Handles peer-to-peer payments and transaction orchestration.

**Key Features:**

- Coordinates wallet debit and credit operations  
- Persists transaction history  
- Publishes transaction events to Kafka  
- Ensures reliable transaction processing in distributed environment  

---

### Reward Service
**Responsibilities:** Processes rewards and cashback asynchronously.

**Key Features:**

- Kafka consumer for transaction events  
- Issues rewards based on transaction activity  
- Idempotent event handling prevents duplicate rewards  

---

### Notification Service
**Responsibilities:** Sends transaction notifications asynchronously.

**Key Features:**

- Kafka consumer for transaction events  
- Fully decoupled from transaction processing  
- Failures do not affect core payment flow  

---

### API Gateway
**Responsibilities:** Entry point for all external requests.

**Key Features:**

- Request routing using Spring Cloud Gateway  
- JWT validation and authentication filtering  
- Rate limiting using Redis  
- Load balancing across service instances  


---

# Installation and Running the Project

## Prerequisites

Ensure the following are installed on your system:

- Java 17+
- Docker
- Docker Compose
- Maven
- IntelliJ IDEA (optional, recommended for development)

---

## Step 1 - Clone repository

```bash
git clone https://github.com/anirban-rudra/PayFlow.git
cd PayFlow

Step 2 - Start infrastructure services
Start Kafka, Zookeeper, Redis, and databases using Docker Compose:
docker-compose up -d

Verify all containers are running:
docker ps

This will start:
Kafka (event messaging system)
Zookeeper (Kafka coordination)
Redis (rate limiting and caching)
PostgreSQL/MySQL (databases)

Step 3 - Start microservices
Start each microservice individually.

Option 1 - Using IntelliJ IDEA (recommended)
Open the project in IntelliJ IDEA.

Run the following services:

user-service
wallet-service
transaction-service
reward-service
notification-service
api-gateway

Option 2 - Using terminal
Start each service using Maven wrapper.

Example:
cd user-service
./mvnw spring-boot:run
Repeat for the following services:

cd wallet-service
./mvnw spring-boot:run

cd transaction-service
./mvnw spring-boot:run

cd reward-service
./mvnw spring-boot:run

cd notification-service
./mvnw spring-boot:run

cd api-gateway
./mvnw spring-boot:run

Step 4 - Access the system
The API Gateway runs on:
http://localhost:8080
All client requests should be sent through the API Gateway.

Step 5 - Verify Redis
Find Redis container name:
docker ps

Then run:
docker exec -it <redis-container-name> redis-cli ping

Expected output:
PONG

Step 6 - Stop services
Stop all infrastructure services:

docker-compose down
Stop microservices using IntelliJ or Ctrl+C in terminal.
