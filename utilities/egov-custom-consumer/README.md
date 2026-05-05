# Custom Consumer Service (egov-custom-consumer)

Custom consumer service invokes a co-existence api to clear the auth token present in co-existence finance erp if the user logs out in the rain maker system.

## Prerequisites

- Java 17
- Maven 3.6+
- Kafka

## Technology Stack

- Java 17
- Spring Boot 3.4.4
- Spring Framework 6.2.6
- Lombok 1.18.38
- Log4j2 2.24.3

## Dependencies

| Dependency | Version |
|---|---|
| spring-boot-starter-web | 3.4.4 (managed) |
| spring-beans | 6.2.6 |
| commons-lang3 | 3.14.0 |
| lombok | 1.18.38 |
| json-path | 2.9.0 |
| org.json | 20240303 |
| jackson-databind | managed |
| tracer | 2.0.0-SNAPSHOT |
| services-common | 1.0.0-RELEASE |
### DB UML Diagram

NA

### Service Dependencies

NA

### Swagger API Contract

NA

## Service Details

Custom Consumer Service (egov-custom-consumer) is a consumer which listens to the res-custom-filter topic, and invokes co-existence rest end ponit to clear the auth token present in the co-existence finance erp.

### API Details

NA

### Kafka Consumers

egov.custom.async.filter.topic : res-custom-filter
	
	listens to this topic to clear the auth token present in the finance co-existence erp.

### Kafka Producers

NA
