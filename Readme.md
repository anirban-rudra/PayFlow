PayPal Clone - Spring Boot Microservices

This is a PayPal Clone Application built using Spring Boot microservices architecture. The project includes multiple services such as User Service, Transaction Service, API Gateway, and uses Redis, Kafka, and Docker Compose for messaging, caching, and orchestration.

🚀 Prerequisites

Before running the application, ensure you have the following installed:

Java 17+

IntelliJ IDEA (for running locally)

Docker & Docker Compose

Kafka & Zookeeper (via Docker Compose)

Redis

🛠️ Running the Application
Step 1: Start Kafka & Zookeeper

Kafka and Zookeeper are configured using Docker Compose. Navigate to the directory containing your docker-compose.yml file:

docker-compose up -d


This will start:

Zookeeper

Kafka broker(s)

Verify running containers:

docker ps

Step 2: Start Other Microservices

You can start each microservice individually using IntelliJ or the command line.

Option 1: Run in IntelliJ

Open the service module in IntelliJ (e.g., user-service, transaction-service).

Click the Run icon or press Shift + F10.

Option 2: Run via Command Line (macOS/Linux)

Navigate to the service folder:

cd user-service


Build and run the service:

./mvnw clean install
./mvnw spring-boot:run


Or run the packaged JAR:

java -jar target/user-service-0.0.1-SNAPSHOT.jar


Repeat for all microservices (transaction-service, reward-service, notification-service).

Step 3: Start Redis (for API Gateway Rate Limiting)

Run Redis using Docker:

docker run -d --name redis -p 6379:6379 redis:alpine


Check running containers:

docker ps


Test Redis connection:

redis-cli ping


✅ Output should be:

PONG

Step 4: Start API Gateway

Important: Redis must be running before starting the API Gateway.

In IntelliJ: Open api-gateway-service module and click Run.

Via Command Line:

cd api-gateway-service
./mvnw clean install
./mvnw spring-boot:run


Or with JAR:

java -jar target/api-gateway-service-0.0.1-SNAPSHOT.jar

⚡ Technologies Used

Spring Boot

Spring Security (JWT Authentication)

Kafka (Messaging)

Redis (Caching & Rate Limiting)

Docker & Docker Compose

Maven

Optional: Stop Docker Containers
docker-compose down
docker stop redis
docker rm redis
