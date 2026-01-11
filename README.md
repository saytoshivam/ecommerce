# E-Commerce Microservices Project

This project implements two Spring Boot microservices for an e-commerce system: **Inventory Service** and **Order Service**.

## Project Structure

```
ecommerce/
├── inventory-service/     # Inventory management microservice
├── order-service/         # Order processing microservice
└── pom.xml               # Parent POM for multi-module project
```

## Prerequisites

- Java 17 or higher
- Maven 3.6+ (or use the included Maven wrapper `./mvnw`)

## Services Overview

### Inventory Service (Port 8081)

Manages product inventory with batch tracking and expiry dates.

**Endpoints:**
- `GET /inventory/{productId}` - Get inventory batches for a product, sorted by expiry date
- `POST /inventory/update` - Update inventory after order placement

**Features:**
- Factory Design Pattern for extensible inventory handling strategies
- FIFO (First In First Out) batch selection based on expiry dates
- H2 in-memory database with Liquibase for schema and data management

### Order Service (Port 8082)

Processes product orders and communicates with Inventory Service.

**Endpoints:**
- `POST /order` - Place a new order

**Features:**
- Inter-service communication using RestTemplate
- Automatic inventory reservation during order placement
- H2 in-memory database with Liquibase for schema and data management

## Setup Instructions

### 1. Build the Project

From the root directory:

```bash
./mvnw clean install
```

Or if you have Maven installed:

```bash
mvn clean install
```

### 2. Run Inventory Service

```bash
cd inventory-service
../mvnw spring-boot:run
```

Or:

```bash
java -jar inventory-service/target/inventory-service-0.0.1-SNAPSHOT.jar
```

The service will start on **http://localhost:8081**

### 3. Run Order Service

In a separate terminal:

```bash
cd order-service
../mvnw spring-boot:run
```

Or:

```bash
java -jar order-service/target/order-service-0.0.1-SNAPSHOT.jar
```

The service will start on **http://localhost:8082**

**Note:** Make sure Inventory Service is running before starting Order Service, as Order Service depends on it.

## API Documentation

### Inventory Service

#### Get Inventory by Product ID

```http
GET /inventory/{productId}
```

**Example Request:**
```bash
curl http://localhost:8081/inventory/1001
```

**Example Response:**
```json
{
  "productId": 1001,
  "productName": "Laptop",
  "batches": [
    {
      "batchId": 1,
      "quantity": 68,
      "expiryDate": "2026-06-25"
    }
  ]
}
```

#### Update Inventory

```http
POST /inventory/update
Content-Type: application/json
```

**Example Request:**
```bash
curl -X POST http://localhost:8081/inventory/update \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1001,
    "quantity": 10
  }'
```

**Example Response:**
```json
{
  "reservedBatchIds": [1],
  "message": "Inventory updated successfully"
}
```

### Order Service

#### Place Order

```http
POST /order
Content-Type: application/json
```

**Example Request:**
```bash
curl -X POST http://localhost:8082/order \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1002,
    "quantity": 3
  }'
```

**Example Response:**
```json
{
  "orderId": 5012,
  "productId": 1002,
  "productName": "Smartphone",
  "quantity": 3,
  "status": "PLACED",
  "reservedFromBatchIds": [9],
  "message": "Order placed. Inventory reserved."
}
```

## Database Access

Both services use H2 in-memory databases. You can access the H2 console:

- **Inventory Service:** http://localhost:8081/h2-console
  - JDBC URL: `jdbc:h2:mem:inventorydb`
  - Username: `sa`
  - Password: (empty)

- **Order Service:** http://localhost:8082/h2-console
  - JDBC URL: `jdbc:h2:mem:orderdb`
  - Username: `sa`
  - Password: (empty)

## Data Loading

Data is automatically loaded via Liquibase changelogs on application startup:

- **Inventory Service:** Loads inventory batches from `inventory-service/src/main/resources/db/changelog/data/inventory.csv`
- **Order Service:** Loads sample orders from `order-service/src/main/resources/db/changelog/data/orders.csv`

## Testing

### Run All Tests

From the root directory:

```bash
./mvnw test
```

### Run Tests for Specific Service

```bash
# Inventory Service
cd inventory-service
../mvnw test

# Order Service
cd order-service
../mvnw test
```

### Test Coverage

The project includes:
- **Unit Tests:** Service layer logic with JUnit 5 and Mockito
- **Integration Tests:** REST endpoints using `@SpringBootTest` and `TestRestTemplate`

## Architecture

### Factory Design Pattern

The Inventory Service implements a Factory Pattern for inventory handling strategies:

- `InventoryHandlerFactory` - Factory for creating inventory handlers
- `InventoryHandler` - Interface for inventory selection strategies
- `FIFOInventoryHandler` - FIFO implementation (first expiry date first)

This design allows easy extension with new strategies (e.g., LIFO, Weighted Average) without modifying existing code.

### Service Communication

Order Service communicates with Inventory Service using `RestTemplate` for:
- Checking inventory availability
- Reserving inventory batches
- Updating inventory quantities

## Technologies Used

- **Spring Boot 3.5.9** - Framework
- **Spring Data JPA** - Data persistence
- **H2 Database** - In-memory database
- **Liquibase** - Database migration and data loading
- **Lombok** - Reducing boilerplate code
- **JUnit 5** - Testing framework
- **Mockito** - Mocking framework
- **RestTemplate** - HTTP client for inter-service communication
- **Swagger/OpenAPI** - API documentation (optional)

## License
This project is part of the Körber Java Microservices Assignment.