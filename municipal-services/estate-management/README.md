# Estate Management Service

## Overview
Estate Management Service is a Spring Boot microservice that manages government estate assets and their allotments. It provides APIs for creating, updating, and searching estate assets and allotments within the UPYOG platform.

## Features
- **Asset Management**: Create, update, and search estate assets
- **Allotment Management**: Create and search asset allotments
- **Integration**: Seamless integration with UPYOG ecosystem services
- **Workflow Support**: Built-in workflow management for asset lifecycle
- **Audit Trail**: Complete audit logging for all operations

## Technology Stack
- **Framework**: Spring Boot 3.2.2
- **Java Version**: 17
- **Database**: PostgreSQL
- **Message Queue**: Apache Kafka
- **Documentation**: OpenAPI 3.0 (Swagger)

## API Endpoints

### Asset Management
- `POST /estate/asset/v1/_create` - Create new asset
- `POST /estate/asset/v1/_update` - Update existing asset
- `POST /estate/asset/v1/_search` - Search assets

### Allotment Management
- `POST /estate/allotment/v1/_create` - Create new allotment
- `POST /estate/allotment/v1/_search` - Search allotments

## Quick Start

### Prerequisites
- Java 17+
- PostgreSQL 12+
- Apache Kafka
- Maven 3.6+

### Configuration
Update `application.properties`:
```properties
server.port=8585
server.servlet.context-path=/estate-management
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=root
```

### Running the Service
```bash
mvn clean install
mvn spring-boot:run
```

### API Documentation
Access Swagger UI at: `http://localhost:8585/estate-management/swagger-ui/index.html`

## Dependencies
- MDMS Service (Master Data)
- ID Generation Service
- User Service
- Workflow Service
- Asset Service
- Billing Service

## Kafka Topics
- `save-asset-details` - Asset creation events
- `update-asset-details` - Asset update events
- `save-allotment-details` - Allotment creation events

## Database
Uses PostgreSQL with Flyway migrations located in `src/main/resources/db/migration/main/`

## Environment Variables
Key configuration properties:
- `server.port` - Service port (default: 8585)
- `spring.datasource.url` - Database connection URL
- `kafka.config.bootstrap_server_config` - Kafka broker URL
- `egov.mdms.host` - MDMS service host

## Development
1. Clone the repository
2. Configure database and Kafka connections
3. Run `mvn clean install`
4. Start the service with `mvn spring-boot:run`

