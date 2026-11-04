# Requester Services DX

## Overview

The `requester-services-dx` is a Spring Boot application designed to act as an integration layer and API gateway for Data Exchange (DX) and external third-party services. It provides core functionalities for integrating with **Aadhaar e-KYC & OTP Verification**, **DigiLocker**, **DigiPin generation**, and **eSign (eMudhra)** document signing capabilities within the UPYOG platform.

---

## Key Features

- **Aadhaar OTP & e-KYC Verification**: Stateless integration gateway supporting citizen Aadhaar OTP dispatch and OTP verification to retrieve authenticated demographic and address details (`UserDemographics`).
- **100% SOLID Architecture**: Pluggable **Adapter Pattern** with Spring's `@ConditionalOnProperty` allowing zero-code runtime switching between **Mock (simulated)** and **Live (third-party vendor REST)** modes.
- **Spring 6 RestClient Integration**: Leverages modern synchronous `RestClient` for reliable, non-reactive HTTP communication with third-party KYC gateways (such as Sandbox India).
- **DigiLocker Integration**: Handles user authorization, token generation, user details retrieval, and fetching issued documents directly from DigiLocker APIs.
- **DigiPin Generation**: Provides endpoints to generate DigiPins based on geographic coordinates.
- **eSign (eMudhra) Integration**: Facilitates electronic document signing via eMudhra, including redirect and callback handling for successful signatures.
- **Data Exchange & Encryption**: Integrates with `egov-enc-service` for cryptographic operations (encryption/decryption).
- **ID Generation**: Integrates with `egov-idgen` service to generate transactional IDs for eSign requests.

---

## Tech Stack

- **Java**: 17
- **Framework**: Spring Boot 3.2.2 (Spring Framework 6.1)
- **HTTP Client**: Spring `RestClient` (Synchronous fluent client)
- **Database**: PostgreSQL
- **Migrations**: Flyway
- **Messaging**: Apache Kafka (Spring Kafka)
- **Tracing**: eGov Tracer, OpenTelemetry
- **Documentation**: SpringDoc OpenAPI (Swagger)

---

## Project Structure

```
org.egov.dx
├── adapter.vendor             # Third-party gateway adapter interfaces & DTOs
│   ├── AadhaarGatewayAdapter  # Core abstraction interface (DIP / ISP)
│   ├── AadhaarGatewayService  # Service facade delegating to active adapter
│   ├── dto                    # Third-party vendor request & response contracts
│   │   ├── VendorSendOtpRequest
│   │   ├── VendorSendOtpResponse
│   │   ├── VendorVerifyOtpRequest
│   │   ├── VendorVerifyOtpSuccessResponse
│   │   └── VendorVerifyOtpFailureResponse
│   └── impl                   # Concrete adapter implementations (OCP / LSP)
│       ├── MockAadhaarGatewayAdapter    # Activated when mock.enabled=true
│       └── SandboxAadhaarGatewayAdapter # Activated when mock.enabled=false (RestClient)
├── validator                  # Dedicated request validators (SRP)
│   └── AadhaarVerificationValidator     # Validates Aadhaar & OTP formats
├── web
│   ├── controller             # REST API controllers
│   │   ├── AadhaarVerificationController # Aadhaar endpoints (/_sendOtp, /_verifyOtp)
│   │   ├── DiginpinController
│   │   └── eMudraController
│   └── models                 # Public UPYOG domain contracts & models
│       ├── AadhaarVerificationRequest
│       ├── AadhaarVerificationResponse
│       ├── UserDemographics
│       └── ResponseInfoFactory
├── util                       # Centralized configurations & constants
│   ├── AadhaarConstants       # Centralized regex patterns, status codes, messages
│   └── Configurations         # Centralized @Value application properties
```

---

## Aadhaar Verification Flow & SOLID Architecture

The Aadhaar verification module adheres strictly to all five **SOLID** principles:

```
[Client / UI / egov-user]
         │
         ▼
[AadhaarVerificationController]  ──(delegates validation)──► [AadhaarVerificationValidator]
         │
         ▼ (depends on abstraction via constructor injection)
[<<interface>> AadhaarGatewayAdapter]
         │
         ├─────────────────────────────────────────┐
         ▼ (when mock.enabled=true)                ▼ (when mock.enabled=false)
[MockAadhaarGatewayAdapter]              [SandboxAadhaarGatewayAdapter]
   (Generates simulated data)               (Spring 6 RestClient -> Sandbox India)
```

### SOLID Mapping:
1. **Single Responsibility Principle (SRP):**
   - `AadhaarVerificationController`: Only handles HTTP request/response orchestration.
   - `AadhaarVerificationValidator`: Isolates format validation (12-digit Aadhaar starting with 2-9, 6-digit OTP).
   - `MockAadhaarGatewayAdapter`: Solely produces simulated dummy responses for local development.
   - `SandboxAadhaarGatewayAdapter`: Solely handles live network HTTP calls via `RestClient`.
   - Domain DTOs (`AadhaarVerification*`) are decoupled from third-party DTOs (`Vendor*`).
2. **Open / Closed Principle (OCP):**
   - New gateway providers (e.g. Protean / NSDL, direct UIDAI AUA/KUA) can be added simply by creating a new class implementing `AadhaarGatewayAdapter` with `@ConditionalOnProperty`. Existing classes require **zero modifications**.
3. **Liskov Substitution Principle (LSP):**
   - `MockAadhaarGatewayAdapter` and `SandboxAadhaarGatewayAdapter` are 100% interchangeable implementations of `AadhaarGatewayAdapter`.
4. **Interface Segregation Principle (ISP):**
   - `AadhaarGatewayAdapter` exposes only the two cohesive methods strictly needed by the verification domain (`sendOtp` and `verifyOtp`).
5. **Dependency Inversion Principle (DIP):**
   - `AadhaarVerificationController` depends exclusively on the `AadhaarGatewayAdapter` abstraction injected via constructor injection, never on concrete classes.

---

## API Endpoints

### 1. Aadhaar Verification Endpoints (`/aadhaar/v1`)

#### A. Send OTP (`POST /aadhaar/v1/_sendOtp`)
Dispatches an OTP to the citizen's Aadhaar-registered mobile number.

- **Request Body:**
```json
{
  "RequestInfo": {
    "apiId": "org.egov.dx",
    "ver": "1.0",
    "ts": 1728468000000,
    "msgId": "msg-001"
  },
  "aadhaarNumber": "548796521458"
}
```

- **Success Response (HTTP 200 OK):**
```json
{
  "ResponseInfo": {
    "apiId": "org.egov.dx",
    "ver": "1.0",
    "ts": 1728468001000,
    "status": "successful"
  },
  "txnId": "TXN-1728468000500",
  "referenceId": "784512",
  "status": "OTP_SENT",
  "message": "OTP sent successfully to registered mobile number"
}
```

---

#### B. Verify OTP (`POST /aadhaar/v1/_verifyOtp`)
Validates the 6-digit OTP against the active session `referenceId` and retrieves verified e-KYC demographics.

- **Request Body:**
```json
{
  "RequestInfo": {
    "apiId": "org.egov.dx",
    "ver": "1.0",
    "ts": 1728468005000,
    "msgId": "msg-002"
  },
  "referenceId": "784512",
  "otp": "123456",
  "isResync": true,
  "userUuid": "usr-12345"
}
```

> **Note on Profile Re-sync:** For re-syncing an existing user profile (e.g. from the UI "Re-sync Profile with Aadhaar" button), clients can pass `"isResync": true` and `"userUuid": "<uuid>"` in both `/_sendOtp` and `/_verifyOtp` requests for audit and correlation logging.

- **Success Response (HTTP 200 OK):**
```json
{
  "ResponseInfo": {
    "apiId": "org.egov.dx",
    "ver": "1.0",
    "ts": 1728468006000,
    "status": "successful"
  },
  "txnId": "TXN-1728468000500",
  "referenceId": "784512",
  "status": "VERIFIED",
  "isVerified": true,
  "maskedAadhaar": "XXXXXXXX1234",
  "message": "Aadhaar verified successfully",
  "demographics": {
    "name": "Citizen Name",
    "dob": "1995-05-15",
    "gender": "M",
    "careOf": "S/O Parent Name",
    "doorNo": "123",
    "street": "Model Town Road",
    "landmark": "Near Central Park",
    "locality": "Tehsil 1",
    "city": "Amritsar",
    "district": "Amritsar",
    "state": "Punjab",
    "pincode": "143001",
    "country": "India",
    "fullAddress": "123, Model Town, City, State, 143001"
  }
}
```

- **Failure Response (HTTP 400 Bad Request):**
```json
{
  "ResponseInfo": {
    "apiId": "org.egov.dx",
    "status": "failed"
  },
  "txnId": "TXN-1728468000500",
  "referenceId": "784512",
  "status": "INVALID_OTP",
  "isVerified": false,
  "message": "Invalid or expired OTP"
}
```

---

### 2. DigiLocker Endpoints (`/digilocker`)
- `POST /authorization/url`: Generates authorization URL for DigiLocker OAuth flow.
- `POST /authorization/url/citizen`: Generates authorization URL specifically for citizens.
- `POST /token`: Exchanges authorization code for an access token.
- `POST /token/citizen`: Exchanges authorization code for an access token for citizens.
- `POST /details`: Fetches user details from DigiLocker.
- `POST /issuedfiles`: Retrieves a list of files issued to the user.
- `POST /file`: Downloads a specific issued file as a PDF.

### 3. DigiPin Endpoints (`/digipin`)
- `POST /v1/_generate`: Generates a new DigiPin based on latitude and longitude coordinates.

### 4. eSign Endpoints (eMudhra)
- Handled primarily by `eMudraController`, providing document signing and redirect callbacks (`/eSign/redirect`).

---

## Configuration

Service properties are configured in `src/main/resources/application.properties`.

### Aadhaar Gateway Configuration Properties

| Property Key | Default Value | Description |
|---|---|---|
| `aadhaar.gateway.mock.enabled` | `true` | **Feature toggle flag**: `true` uses simulated mock responses; `false` uses live `RestClient` calling Sandbox India. |
| `aadhaar.gateway.base.url` | `https://api.sandbox.co.in` | Base URL for the third-party KYC gateway provider. |
| `aadhaar.gateway.send.otp.path` | `/kyc/aadhaar/okyc/otp` | Path for the vendor Send OTP endpoint. |
| `aadhaar.gateway.verify.otp.path` | `/kyc/aadhaar/okyc/otp/verify` | Path for the vendor Verify OTP endpoint. |
| `aadhaar.gateway.api.key` | `""` | Vendor authentication API key (`x-api-key`). |
| `aadhaar.gateway.api.secret` | `""` | Vendor authentication API secret (`x-api-secret`). |
| `aadhaar.gateway.api.version` | `1.0` | Vendor API version (`x-api-version`). |
| `aadhaar.otp.max.attempts` | `3` | Maximum allowed failed OTP verification attempts per reference ID before locking the session. |

---

## Security, Rate Limiting & PII Protection

In adherence to UIDAI, NIUA, and UPYOG privacy and security guidelines:
- **Replay Attack Prevention**: Verifies that each OTP verification session (`referenceId` / `txnId`) can only be consumed once. Re-submitting a consumed `referenceId` is immediately rejected with `ERR_CODE_OTP_ALREADY_VERIFIED` (`MSG_OTP_ALREADY_VERIFIED`).
- **UIDAI Verhoeff Checksum Algorithm**: Validates the 12th digit checksum of Aadhaar numbers using dihedral group D5 (`VerhoeffAlgorithm.java`) before dispatching OTP to vendor gateways (`ERR_CODE_INVALID_AADHAAR_CHECKSUM`). Detects all single-digit entry errors and adjacent transposition errors, and rejects trivial repetitive numbers (e.g. `222222222222`).
- **Distributed Multi-Pod Rate Limiting & Replay Cache**: Backed by Redis keys (`aadhaar:otp:attempts:{refId}` and `aadhaar:otp:consumed:{refId}`) with in-memory concurrent data structure fallback if Redis is temporarily unreachable.
- **Brute-Force & OTP Lockout Protection**: OTP verification attempts per `referenceId` are tracked. If invalid attempts reach `aadhaar.otp.max.attempts` (default `3`), the transaction is locked (`OTP_MAX_ATTEMPTS_EXCEEDED`), the cached reference is evicted, and the user must request a fresh OTP.
- **Year-Only DOB (YOB) Normalization**: Standardizes UIDAI e-KYC responses where DOB is only returned as a 4-digit birth year (e.g., `"1965"` or `"00/00/1965"`) by normalizing it into `"YYYY-01-01"` to satisfy downstream date parsing requirements.
- **Granular Rural Address Fallback**: Automatically defaults missing or sparse rural address fields (`doorNo`, `street`, `landmark`, `locality`, `city`) to prevent downstream validation failures when storing citizen profiles.
- **Aadhaar Masking**: Raw 12-digit Aadhaar numbers are masked in all logs as `XXXXXXXX1234` (`maskAadhaar`).
- **OTP Protection**: One-time passwords (`otp`) are excluded from `@ToString` in all request models to prevent accidental logging.
- **Payload Sanitization**: Base64 photo strings are excluded from `@ToString` in demographic models to avoid log bloat and memory pressure.

---

### Key External Service Dependencies:
1. **PostgreSQL Database**:
   - `spring.datasource.url`: Database connection URL.
   - `spring.flyway.enabled`: Set to `true` to run migrations on startup.
2. **Kafka**:
   - `kafka.config.bootstrap_server_config`: Typically `localhost:9092`.
   - Topics: `save-tl-esign-txns`, `update-tl-esign-txns`.
3. **External Services**:
   - `egov-enc-service` (Encryption)
   - `egov-user` (User authentication)
   - `egov-idgen` (ID Generation)
   - `api.digitallocker.gov.in` (DigiLocker API)
   - `api.sandbox.co.in` (Aadhaar OKYC Gateway)

---

## Build and Run

### Prerequisites
- Java 17
- Maven 3.x
- Running instances of PostgreSQL and Kafka (if using eSign persistence).

### Build the Project
```bash
mvn clean install -DskipTests
```

### Run the Application
You can run the application directly using Maven:
```bash
mvn spring-boot:run
```

Or run the built JAR:
```bash
java -jar target/requester-services-dx-1.1.6-SNAPSHOT.jar
```

The application starts by default on port `8280` with the context path `/requester-services-dx`.
