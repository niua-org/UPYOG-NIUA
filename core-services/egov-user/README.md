# User Service (egov-user)

<p>User Service (egov-user) is used for user data management and providing functionality to login and logout into UPYOG system </p>

### DB UML Diagram

- NA

### Service Dependencies

- MDMS Service (egov-mdms-service)
- Encryption Service (egov-enc-service)
- OTP Service (egov-otp)
- Filestore Service (egov-filestore)


### Swagger API Contract

- https://editor.swagger.io/?url=https://raw.githubusercontent.com/upyog/UPYOG/master/core-services/docs/egov-user-contract.yml#!/

## Service Details

Feature List:
- Employee:
  - User registration
  - Search user
  - Update user details
  - Forgot password
  - Change password
  - User role mapping(Single ulb to  multiple role)
  - Enable employee to login into UPYOG system based on password.

- Citizen:
  - Create user
  - Update user
  - Search user
  - User registration using OTP
  - OTP based login


#### Configurations

NA

### API Details


a) `POST /citizen/_create`

Create citizen with OTP validation. If `citizen.registration.withlogin.enabled` property in applications.properties is `true` then created citizen would be logged in automatically and he would get information to access platform services, ex:- auth token, refresh token etc.

b) `POST /users/_createnovalidate`

Create user without any otp validation.

c) `POST /_search`

End-point to search the users by providing userSearchRequest. In Request if there is no active filed value, it will fetch only active users.
The available search parameters are more in interservice call as compared to call coming externally.

d) `POST /v1/_search`

Similar to `/_search` endpoint except there is no default value provided for search active/inactive users.

e) `POST /_details`

End-point to fetch the user details by access-token

f) `POST /users/_updatenovalidate`

End-point to update the user details without OTP validations. User's username, type and tenantId are not updated and ignored in update.

g) `POST /profile/_update`

End-point to update user profile. This allows partial update on user's account.

h) `POST /password/_update`

End-point to update the password for loggedInUser. The existing password is validated before updating new password.

i) `POST /password/nologin/_update`

End-point to update the password for non logged in user. The OTP is validated before updating new password.

j) `POST /_logout`

Endpoint to logout session

k) `POST /user/oauth/token`

Endpoint for login. If the user is citizen the login is otp based else it is password based.

l) `POST /user/users/v2/_login`

**Multi-Auth Orchestration (`/users/v2/_login`):**
Supports seamless citizen authentication across 3 distinct channels with automated OTP verification and account provisioning:
1. **`MOBILE` Channel:**
   - Authenticates via Mobile Number + SMS OTP (validated against `egov-otp` service or fixed OTP in development).
   - **Edge Case - Multiple Citizens linked to Single Mobile Number:**
     - In India, multiple family members often link their profiles/Aadhaar cards to a shared mobile number.
     - When multiple citizen profiles are linked to the provided mobile number, the API returns a `200 OK` response with response message `uip-loginv2-multiple-users` containing a lightweight summary of all accounts (`uuid`, `name`, `maskedAadhaar`, `gender`, `dob`).
     - The citizen selects their specific account on the UI and re-submits with `selectedUserUuid`.
     - Upon selection, the token generator issues the OAuth2/JWT access token for that specific citizen.
   - If no profile exists, automatically provisions a lightweight citizen profile.
2. **`AADHAAR` Channel:**
   - Authenticates via 12-digit Aadhaar Number or `referenceId` / `txnId` + UIDAI OTP.
   - Delegates verification to `requester-services-dx` (`/aadhaar/v1/_verifyOtp`), fetches verified e-KYC demographics (`name`, `dob`, `gender`, `address`).
   - Sets `is_identity_verified = true`, `identity_verified_date = NOW()`, `last_resync_date = NOW()`, `last_login_method = AADHAAR`.
   - Proactively auto-provisions or links verified citizen profile.
3. **`FAMILY_ID` Channel:**
   - Authenticates using Household Family ID + Member Mobile Number + OTP.
   - Links individual citizen profile with the family construct.

Returns unified OAuth2/JWT token, refresh token, Redis-persisted session data, and comprehensive `UserRequest` contract.

---

### Section 8: Citizen Profile Field Governance Matrix

The profile management APIs strictly enforce the Section 8 Business Rules defining mandatory fields on profile creation and field editability on profile updates:

| Field Name | Profile Creation | Profile Update | Notes / Restrictions |
|---|---|---|---|
| **Name\*** | Mandatory | Non-Editable | Official legal name. Locked once created; modified only via Aadhaar Re-sync. |
| **Father’s Name\*** | Mandatory | Non-Editable | Mandatory parent detail. Immutable on manual update. |
| **Mother’s Name\*** | Mandatory | Non-Editable | Mandatory parent detail. Immutable on manual update. |
| **Spouse Name** | Non-Mandatory | **Editable** | Citizen may add or update spouse details anytime. |
| **Guardian Name** | Non-Mandatory | **Editable** | Citizen may update guardian details anytime. |
| **Mobile Number\*** | Mandatory | Non-Editable | Core account identifier and OTP channel. Immutable. |
| **Alternate Mobile Number** | Non-Mandatory | **Editable** | Additional contact number. Fully editable. |
| **Date of Birth\*** | Mandatory | Non-Editable | Official DOB. Locked once created; modified only via Aadhaar Re-sync. |
| **Email ID\*** | Mandatory | Non-Editable | Official primary email identifier. Immutable. |
| **Gender\*** | Mandatory | Non-Editable | Official gender. Locked once created; modified only via Aadhaar Re-sync. |
| **Upload Profile Picture** | Non-Mandatory | **Editable** | Profile avatar image. Fully editable. |
| **City\*** | Mandatory | Non-Editable | Locked per address record. To change, create a new address. |
| **Locality\*** | Mandatory | Non-Editable | Locked per address record. To change, create a new address. |
| **Street Name\*** | Mandatory | **Editable** | Street/road details are editable on existing address. |
| **House Number\*** | Mandatory | Non-Editable | Door/House number is non-editable. |
| **House Name** | Non-Mandatory | **Editable** | Building/society/estate name is editable. |
| **Landmark\*** | Mandatory | **Editable** | Nearby landmark is editable. |
| **Pin Code\*** | Mandatory | Non-Editable | Postal code is non-editable. |
| **Latitude / Longitude** | Non-Mandatory | **Editable** | Geographic coordinates can be updated. |
| **Address Type\*** | Mandatory | **Editable** | e.g., `PERMANENT`, `CORRESPONDENCE`, `OTHER`. |
| **Upload Address Proof\*** | Mandatory | **Editable** | Mandatory on profile creation; proof document can be updated. |
| **Upload Identity Proof\*** | Mandatory | **Editable** | Mandatory on profile creation; proof document can be updated. |
| **Upload D.O.B. Proof\*** | Mandatory | **Editable** | Mandatory on profile creation; proof document can be updated. |

---

### Key Workflows & Architectural Principles

#### 1. Citizen Profile Re-Sync & Security Workflow (`POST /user/profile/_resync`)
- **UI Trigger**: Citizen clicks "Re-sync with Aadhaar" on the profile page.
- **Authentication Check**: Enforces that only the authenticated user holding a valid OAuth token can re-sync their own profile. The API compares `requestInfo.userInfo.uuid` with `existingUser.uuid`. Any attempt by Citizen A to re-sync Citizen B's profile is blocked (`ERROR_UNAUTHORIZED_RESYNC`).
- **Aadhaar Match Check**: Validates that the provided Aadhaar number matches the citizen's previously registered Aadhaar number (last-4 digits check), preventing identity takeover.
- **Demographic Refresh**: Verifies Aadhaar OTP with UIDAI gateway via `requester-services-dx`, safely overwriting verified `name`, `dob`, and `gender`, updating `last_resync_date = NOW()`, and incrementing `editted_count`.

#### 2. Address Lifecycle & Type Constraints (`/user/address/*`)
- **Single Active `PERMANENT` Address**: A citizen can have only **one** active `PERMANENT` address at any time. When a new `PERMANENT` address is created or updated, any existing active `PERMANENT` address is automatically soft-deactivated (`status = 'inactive'`).
- **Multiple Active Secondary Addresses**: For other address types (`CORRESPONDENCE`, `OTHER`), citizens are permitted to maintain multiple active address records concurrently.
- **Duplicate Prevention**: Rejects duplicate address submissions if identical details already exist for the user.

#### 3. SCD Type 2 Soft-Versioning Pattern (Audit Retention)
To avoid data loss and preserve an immutable historical audit trail without needing separate audit tables:
- **Address Updates (`updateAddress`)**: Rather than updating records in-place, the service marks the previous address record as `status = 'inactive'` and inserts a new address row with `status = 'active'` and an incremented version number (`version = previousVersion + 1`).
- **Document Updates (`updateDocument`)**: Previous document versions are soft-deactivated (`active = false`), and the updated document is inserted as a new active record (`active = true`).

#### 4. FileStore Verification
- Document attachments and address proof uploads validate `filestoreId` against `egov-filestore` service before persistence. Non-existent or inaccessible files throw `ERROR_FILESTORE_NOT_FOUND`.

#### 5. Configurable OTP Rate Limiting & Lockout
- **Rate Limit Threshold**: Configurable via `aadhaar.otp.max.attempts: 3` and `otp.max.invalid.attempts: 3` in `application.properties`.
- **Lockout Rule**: Tracks invalid OTP attempts per transaction reference (`refId`/`txnId`) and mobile number. Upon the 3rd consecutive failed attempt, the transaction is invalidated and locked, rejecting further attempts with `ERROR_MAX_OTP_ATTEMPTS_EXCEEDED` until a new OTP is requested.

#### 6. Storage Security & PII Protection
- **Masked Aadhaar**: Persistent storage (`eg_user.aadhaarnumber`, `ug_user_document.document_uid`) only stores masked Aadhaar numbers (`XXXXXXXX1234`).
- **Transparent Encryption**: PII data fields are encrypted at rest using `egov-enc-service` with dedicated keys (`USER_ENCRYPTION_KEY`, `USER_ADDRESS_ENCRYPTION_KEY`, `USER_DOCUMENT_ENCRYPTION_KEY`).

#### 7. Centralized Constants Architecture (`UserConstants`)
- All hardcoded strings, error codes, validation messages, encryption keys, and external endpoints are centralized in [`UserConstants.java`](file:///Users/goutamgupta/Desktop/project-fork/UPYOG-NIUA-Goutam/core-services/egov-user/src/main/java/org/egov/user/domain/service/utils/UserConstants.java).

#### 8. Citizen Profile Completeness UX Governance (`isProfileCompleted`)
- Evaluates citizen profile completeness across Section 8 mandatory fields (`name`, `fatherName`, `motherName`, `mobileNumber`, `dob`, `emailId`, `gender`).
- The calculated boolean flag `isProfileCompleted` is returned in `LoginV2Response` and embedded in the auth `User` contract (`UserRequest`), allowing the frontend UI to instantly detect partial/skipped profiles upon login and display a completion banner.

#### 9. Minor Citizen (< 18 Years) Guardian Enforcement
- For citizens under 18 years of age (computed dynamically from `dob`), `guardian` (Guardian Name) and `guardianRelation` (e.g., `FATHER`, `MOTHER`, `OTHER`) are strictly mandatory during profile creation (`ERROR_GUARDIAN_MANDATORY_FOR_MINOR`).
- Guardian details cannot be cleared or removed on subsequent updates while the citizen remains a minor.

#### 10. Alternate Mobile Number Validation Rules
- `alternateMobileNumber` is validated against standard Indian 10-digit mobile pattern (`^[6-9][0-9]{9}$`) (`ERROR_INVALID_ALTERNATE_MOBILE`).
- `alternateMobileNumber` cannot be identical to the citizen's primary `mobileNumber` (`ERROR_ALTERNATE_MOBILE_SAME_AS_PRIMARY`).

#### 11. Cross-Citizen Aadhaar Uniqueness
- Strictly enforces 1-to-1 citizen-to-Aadhaar identity mapping across `createProfile`, `updateProfile`, and `resyncProfile`.
- Searching `eg_user` verifies that no other citizen UUID already possesses the given masked or plain Aadhaar number (`ERROR_AADHAAR_ALREADY_LINKED`).

#### 12. Shared Mobile Multi-Account Auto-Provisioning
- In households sharing a single family mobile number, Aadhaar login cleanly determines whether an existing account on that mobile belongs to the incoming citizen. If all existing profiles on the shared phone already have different Aadhaar numbers bound, the service automatically provisions a new individual profile for that citizen on the shared phone without overwriting family members' accounts.

#### 13. Interactive API Documentation (Swagger / SpringDoc OpenAPI)
- **Swagger UI**: Accessible at `http://<host>:<port>/user/swagger-ui.html`
- **OpenAPI JSON**: Accessible at `http://<host>:<port>/user/v3/api-docs`

---

### Extended Demographic, Verification & Login Columns (Flyway V20261014113000)

| Column Name | Type | Description |
|---|---|---|
| `father_name` | VARCHAR(100) | Citizen's father name |
| `mother_name` | VARCHAR(100) | Citizen's mother name |
| `spouse_name` | VARCHAR(100) | Citizen's spouse name |
| `is_identity_verified` | BOOLEAN | Indicates Aadhaar/e-KYC verified identity (`DEFAULT FALSE`) |
| `identity_verified_date` | TIMESTAMP | Timestamp when identity was verified |
| `last_resync_date` | TIMESTAMP | Timestamp of latest Aadhaar e-KYC re-sync |
| `editted_count` | INT | Count of citizen profile updates (`DEFAULT 0`) |
| `last_login_method` | VARCHAR(32) | Last authentication channel used (`MOBILE`, `AADHAAR`, `FAMILY_ID`, `PASSWORD`, `MOBILE_OTP`) |

### Kafka Producers

- `audit_data` : used in `kafka.topic.audit` application property for logging user data decryption calls.
