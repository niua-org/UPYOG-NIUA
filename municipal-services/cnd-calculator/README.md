# CND Calculator Service

A Spring Boot microservice for calculating Construction and Demolition (CND) waste management fees and generating billing demands within the UPYOG platform.

## Overview

The CND Calculator service is part of the UPYOG municipal services ecosystem, designed to:
- Calculate fees for Construction and Demolition waste management applications
- Generate billing demands based on configurable tax heads
- Integrate with the broader DIGIT platform for municipal governance

## Features

- **Fee Calculation**: Automated calculation of CND waste management fees it takes totalwastequantity and the amount from MDMS
- **Demand Generation**: Creates billing demands for payment processing
- **API Documentation**: Swagger/OpenAPI 3.0 integration for API exploration
- **Database Integration**: PostgreSQL with Flyway migrations
- **Kafka Integration**: Event-driven architecture support
- **MDMS Integration**: Master data management system connectivity

## Technology Stack

- **Java**: 17
- **Spring Boot**: 3.2.2
- **Database**: PostgreSQL 42.7.1
- **Build Tool**: Maven
- **Documentation**: SpringDoc OpenAPI 3.0
- **Messaging**: Apache Kafka
- **Migration**: Flyway

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL database
- Apache Kafka (for event processing)

## Quick Start

### 1. Clone and Build
```bash
git clone https://github.com/upyog/UPYOG
cd municipal-services/cnd-calculator
mvn clean install
```

### 2. Configure Database
Update `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/your_database
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 3. Run the Application
```bash
mvn spring-boot:run
```

The service will start on port `8181` with context path `/cnd-calculator`.

## API Documentation

Once running, access the API documentation at:
- **Swagger UI**: http://localhost:8181/cnd-calculator/swagger-ui/index.html#/
## Key Endpoints

### Calculate CND Fees
```
POST /cnd-calculator/v1/_calculate
```
Calculates fees and generates billing demands for CND applications.

**Request Body**: `CalculationRequest`
**Response**: `DemandResponse` with calculated demands

## Configuration

### Application Properties
Key configuration parameters in `application.properties`:

```properties
# Server Configuration
server.port=8181
server.contextPath=/cnd-calculator

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/database
spring.datasource.username=username
spring.datasource.password=password

# External Services
egov.billingservice.host=http://localhost:8077
egov.cnd.host=http://localhost:8081
egov.mdms.host=http://localhost:8094
```

### Environment Variables
- `DB_URL`: Database connection URL
- `DB_USERNAME`: Database username  
- `DB_PASSWORD`: Database password
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka broker addresses

## Development

### Project Structure
```
src/main/java/org/upyog/cdwm/calculator/
├── config/          # Configuration classes
├── service/         # Business logic services
├── web/
│   ├── controllers/ # REST controllers
│   └── models/      # Request/Response models
├── repository/      # Data access layer
├── util/           # Utility classes
└── kafka/          # Kafka producers/consumers
```

### Running Tests
```bash
mvn test
```

### Building JAR
```bash
mvn clean package
```
Generates `target/cnd-calculator-2.0.0.jar`

## Integration

This service integrates with:
- **Billing Service**: For demand creation and management
- **CND Service**: For application data retrieval
- **MDMS**: For master data like tax heads and amounts
- **Workflow Service**: For application state management

## Contributing

1. Follow existing code style and patterns
2. Add tests for new functionality
3. Update documentation for API changes
4. Ensure all tests pass before submitting

## Support

For issues and questions:
- Check the API documentation at `/swagger-ui.html`
- Review application logs for error details
- Verify database connectivity and external service availability