# Asset Service

[![Version](https://img.shields.io/badge/version-2.0.0-blue.svg)](CHANGELOG.md)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.java.net/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.4-green.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

The **Asset Service** is part of the UPYOG application, providing a comprehensive digital interface for employees to register, manage, assign, dispose, and maintain assets for Urban Local Bodies (ULBs). This service has been upgraded to support LTS (Long Term Support) with modern Java and Spring Boot versions.

---

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+
- PostgreSQL 12+
- Apache Kafka 2.8+

### Running the Service
```bash
# Clone the repository
git clone <repository-url>
cd asset-services

# Build the application
mvn clean compile

# Run the application
mvn spring-boot:run
```

The service will start on `http://localhost:8098`

---

## 📋 Introduction

The **UPYOG Asset Management Module** empowers employees to manage comprehensive asset lifecycle efficiently. The module provides the following functionalities:

### Core Features
- **Asset Registration**: Complete asset registration with documents and address details
- **Asset Search & Management**: Advanced search capabilities with multiple filters
- **Asset Assignment**: Assign assets to employees with full audit trail
- **Asset Disposal**: Manage asset disposal process with approval workflows
- **Asset Maintenance**: Track maintenance activities and schedules
- **Depreciation Calculation**: Automated asset depreciation processing
- **Workflow Integration**: Complete workflow support for all asset operations
- **Document Management**: Attach and manage documents for all asset operations

### UPYOG Services Integration
- **eGov-mdms**: Master data management
- **eGov-persister**: Data persistence layer
- **eGov-idgen**: ID generation service
- **eGov-user**: User management
- **eGov-localization**: Multi-language support
- **eGov-workflow-service**: Workflow management
- **eGov-filestore**: Document storage

---

## 🏗️ Architecture

### Technology Stack
- **Framework**: Spring Boot 3.4.4
- **Java Version**: 17 (LTS)
- **Database**: PostgreSQL
- **Message Queue**: Apache Kafka
- **Build Tool**: Maven
- **API Documentation**: OpenAPI 3.0 (Swagger)
- **Validation**: Jakarta Bean Validation

### Key Dependencies
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
```

---

## 📚 API Documentation

### Asset Management APIs

#### Core Asset Operations
- **`POST /v1/assets/_create`**: Create new asset
- **`POST /v1/assets/_update`**: Update existing asset
- **`POST /v1/assets/_search`**: Search assets with filters

#### Asset Assignment APIs
- **`POST /v1/assets/assignment/_create`**: Create asset assignment
- **`POST /v1/assets/assignment/_update`**: Update asset assignment
- **`POST /v1/assets/assignment/_search`**: Search asset assignments

#### Asset Disposal APIs
- **`POST /v1/disposal/_create`**: Create asset disposal
- **`POST /v1/disposal/_update`**: Update asset disposal
- **`POST /v1/disposal/_search`**: Search asset disposals

#### Asset Maintenance APIs
- **`POST /maintenance/v1/_create`**: Create maintenance record
- **`POST /maintenance/v1/_update`**: Update maintenance record
- **`POST /maintenance/v1/_search`**: Search maintenance records

#### Depreciation APIs
- **`POST /v1/assets/depreciation/_process`**: Trigger depreciation calculation
- **`POST /v1/assets/depreciation/list`**: Get depreciation history



---

## 🗄️ Database Schema

### Core Tables
- **`eg_asset_assetdetails`**: Main asset information
- **`eg_asset_document`**: Asset documents
- **`eg_asset_addressDetails`**: Asset address information
- **`eg_asset_assignmentdetails`**: Asset assignments
- **`eg_asset_disposal_details`**: Asset disposal records
- **`eg_asset_maintenance_details`**: Asset maintenance records
- **`eg_asset_auditdetails`**: Audit trail

### Database Migration
The service uses Flyway for database migrations. Migration scripts are located in:
```
src/main/resources/db/migration/main/
```

---

## ⚙️ Configuration

### Application Properties
```properties
# Server Configuration
server.port=8098
server.servlet.context-path=/asset-services

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/upyog
spring.datasource.username=postgres
spring.datasource.password=postgres

# Kafka Configuration
spring.kafka.bootstrap-servers=localhost:9092

# MDMS Configuration
egov.mdms.host=http://localhost:8094

# ID Generation Configuration
egov.idgen.host=http://localhost:8088
```

### Environment Variables
- `DB_HOST`: Database host
- `DB_PORT`: Database port
- `DB_NAME`: Database name
- `KAFKA_BROKERS`: Kafka broker list
- `MDMS_HOST`: MDMS service host

---

## 🔧 Development

### Building the Application
```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Package
mvn package

# Skip tests during build
mvn package -DskipTests
```

### Running Locally
```bash
# Using Maven
mvn spring-boot:run

# Using Java
java -jar target/asset-services-2.0.0.jar

# With custom profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### API Documentation
Once the service is running, access the API documentation at:
- **Swagger UI**: `http://localhost:8098/asset-services/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8098/asset-services/v3/api-docs`

---

## 🧪 Testing

### Running Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=AssetServiceTest

# Run with coverage
mvn test jacoco:report
```

### Sample Test Data
Test data and sample requests are available in:
```
src/test/resources/
```

---



## 🔍 Monitoring & Logging

### Health Check
```bash
curl http://localhost:8098/asset-services/actuator/health
```

### Metrics
```bash
curl http://localhost:8098/asset-services/actuator/metrics
```

### Logging Configuration
Logging is configured in `application.properties`:
```properties
logging.level.org.egov.asset=DEBUG
logging.level.org.springframework.web=INFO
```

---



## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 📞 Support

For support and questions:
- Create an issue in the repository
- Contact the development team
- Check the [CHANGELOG.md](CHANGELOG.md) for recent updates

---

## 🔄 Migration Guide

For upgrading from version 1.x to 2.0.0, see the [Migration Guide](MIGRATION.md).

### Key Changes in v2.0.0
- Upgraded to Java 17
- Spring Boot 3.4.4
- Jakarta EE migration
- Enhanced API endpoints
- Improved error handling
- Better audit trail support