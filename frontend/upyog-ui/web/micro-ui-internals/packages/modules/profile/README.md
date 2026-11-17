# Profile Module — `@nudmcdgnpm/upyog-ui-module-profile`

**Authors:** Shivank, Parvesh, Ashish  
**Maintained by:** NUDM (National Urban Digital Mission)  
**Package:** `@nudmcdgnpm/upyog-ui-module-profile`  
**Version:** `1.0.0`

---

## Overview

The Profile Module is a citizen-facing module in the UPYOG platform that handles:

- Viewing and editing a citizen's personal profile
- Aadhaar-based identity verification
- Family creation and management (PPP — Parivar Pehchaan Patra)
- Multi-step data collection: Personal Details → Property → Income → Vehicle

---

## Module Structure

```
profile/
└── src/
    ├── Module.js                    ← Entry point, registers all components
    ├── index.js                     ← Citizen app router (React Router v6)
    │
    ├── components/                  ← Profile view & edit components
    │   ├── ProfileDetails.js        ← Main profile view page
    │   ├── EditProfile.js           ← Edit page with tab switcher (Profile / Address / Documents)
    │   ├── EditProfileForm.js       ← Personal details form
    │   ├── EditAddress.js           ← Address edit form
    │   ├── EditDocuments.js         ← Document upload/update
    │   ├── AddressTranformer.js     ← Address data transformer utility
    │   └── UploadDrawer.js          ← Profile photo upload drawer (camera/gallery)
    │
    ├── ppp/                         ← Create Family multi-step flow (PPP)
    │   ├── index.js                 ← FamilyCreate — step router & session manager
    │   ├── CreateProfile.js         ← Step 1: Personal Details
    │   ├── PropertyDetails/
    │   │   ├── PropertyDetails.js   ← Step 2: Property (Urban/Rural selector)
    │   │   ├── UrbanForm.js         ← Urban property sub-form (search & link Property ID)
    │   │   └── RuralForm.js         ← Rural property sub-form (Gram Panchayat, Village, Ward)
    │   ├── IncomeDetails.js         ← Step 3: Income, Tax ID, Occupation
    │   ├── VehicleDetails.js        ← Step 4: Vehicle RC, Type, Fuel
    │   └── DocumentDetails.js       ← Document upload step (reuses EditDocuments)
    │
    ├── familyFeatureComponents/     ← Family management screens
    │   ├── FamilyDashboard.js       ← View all family members
    │   ├── AddFamilyMember.js       ← Add member via Aadhaar verification
    │   ├── FamilyMemberVerified.js  ← Review verified member details
    │   ├── FamilyMemberAdded.js     ← Confirmation after member staged
    │   ├── FamilyCreatedResponse.js ← Success screen after family creation
    │   ├── JoinExistingFamily.js    ← Join a family using Family ID
    │   ├── FoundFamily.js           ← Preview family before joining
    │   └── FamilyJoinedResponse.js  ← Success screen after joining
    │
    ├── verifications/               ← Aadhaar & profile verification
    │   ├── AadhaarVerification.js   ← Reusable Aadhaar OTP widget
    │   ├── OtpVerify.js             ← OTP input handler
    │   └── ProfileVerify.js         ← Profile verification entry screen
    │
    ├── config/
    │   ├── createfamilyconfig.js    ← Step config for Create Family flow
    │   └── FamilyCreateTabs.js      ← Tab navigator (auto-derived from config)
    │
    └── utils/
        ├── index.js                 ← Shared utilities, hooks, presentational components
        ├── constants.js             ← Route constants, field lengths
        ├── payloads.js              ← API payload builders
        ├── profileStyles.js         ← Inline style definitions
        └── profileValidators.js     ← Field validators (name, email, mobile)
```

---

## Routes

All routes are registered under `/upyog-ui/citizen/profile/`:

| Path | Component | Description |
|---|---|---|
| `profile-details` | `ProfileDetails` | View profile, Aadhaar badge, Family dropdown |
| `edit-details` | `EditProfile` | Edit profile / address / documents (tabbed) |
| `verify-profile` | `VerifyProfile` | Aadhaar verification entry |
| `join-family` | `JoinExistingFamily` | Join a family using Family ID |
| `found-family` | `FoundFamily` | Preview family before joining |
| `family-joined` | `FamilyJoinedResponse` | Success after joining |
| `add-family-member` | `AddFamilyMember` | Add member via Aadhaar |
| `family-member-verified` | `FamilyMemberVerified` | Review verified member |
| `family-member-added` | `FamilyMemberAdded` | Member staged confirmation |
| `family-created` | `FamilyCreatedResponse` | Family creation success |
| `family-dashboard` | `FamilyDashboard` | View all family members |
| `create-family/*` | `FamilyCreate` | Multi-step Create Family flow |

---

## Create Family Flow (PPP)

The Create Family flow is a 4-step multi-step form managed by `FamilyCreate` (`ppp/index.js`).

```
Step 1: profileDetails   →  CreateProfile.js
Step 2: propertyDetails  →  PropertyDetails.js  (Urban / Rural sub-forms)
Step 3: incomeDetails    →  IncomeDetails.js
Step 4: vehicles         →  VehicleDetails.js
              ↓
    FamilyMemberAdded    →  Redirects to /family-member-added
```

### How it works

- Step config is defined in `createfamilyconfig.js` — each step has a `route`, `component`, `key`, and `nextStep`.
- `FamilyCreate` reads the config, renders each step as a `<Route>`, and manages navigation via `goNext()`.
- Form data is persisted per-member in `sessionStorage` under the key `Family_Details` using `Digit.Hooks.useSessionStorage`.
- The `?member=<uuid>` query param scopes data to a specific family member — the logged-in user by default, or an Aadhaar-verified member when adding someone else.
- On the last step (`nextStep: null`), the member is staged into the family list and the flow redirects to `family-member-added`.

### Adding Another Member

1. Citizen goes to `add-family-member`.
2. `AddFamilyMember` uses `AadhaarVerification` to verify the new member's Aadhaar.
3. On success, verified member data is cached in `sessionStorage` (`Digit.family.verifiedMember`).
4. The same Create Family steps run again with `?member=<newMemberUuid>`.
5. On completion, `stageVerifiedMember()` adds the member to the staged list.

---

## Key Components

### `ProfileDetails`
- Fetches the logged-in user via `Digit.UserService.userSearch` with `include: ["ADDRESS", "PROFILE", "DOCUMENT"]`.
- Shows Aadhaar verified / unverified badge.
- Shows Family ID + HOF badge if the citizen is Head of Family.
- Renders a "My Family" dropdown from MDMS (`FamilyCreateType → FamilyOptions`) — options are filtered based on whether the citizen already has a `familyId`.
- Blocks family actions if Aadhaar is not verified (shows Toast error).

### `EditProfile`
- Tab-based layout: Profile | Address | Documents.
- Delegates to `EditProfileForm`, `EditAddress`, `EditDocuments`.
- Fetches address as fallback if navigated directly (no `routeState`).
- Follows Single Responsibility Principle — each tab is a separate component.

### `AadhaarVerification`
- Reusable widget used in both `ProfileVerify` and `AddFamilyMember`.
- Formats Aadhaar input as `XXXX XXXX XXXX`.
- Sends OTP via `Digit.UserService.sendAadhaarOtp`.
- 30-second countdown timer with resend.
- Verifies OTP via `Digit.UserService.verifyAadhaarOtp`.
- Calls `onVerified({ verifiedMember, aadhaarLast4, maskedMobile })` on success.

### `FamilyDashboard`
- Fetches family data from `/family/v1/dashboard/_search`.
- Displays all active members with avatar initials, relationship, and verified badge.
- Highlights the logged-in user with a "You" label.

### `PropertyDetails` (Step 2)
- Citizen selects **Urban** or **Rural** locality type.
- **Urban** (`UrbanForm`): Search and link an existing Property ID from the PT module. Supports multiple properties with one marked as primary address.
- **Rural** (`RuralForm`): Collects Gram Panchayat, Village, Ward, Property Type, Usage, Survey Number, Ownership Type, and Address Proof document upload.

### `IncomeDetails` (Step 3)
- Collects Occupation, Annual Income, Tax ID Type (PAN/TAN/GST), Tax ID Number with inline validation, Income Proof upload, and Tax Proof upload (shown only if Tax ID validation fails).

### `VehicleDetails` (Step 4)
- Collects Vehicle Type, Category, Fuel Type, Vehicle Number (with regex validation + validate button), Purchase Year, and RC document upload.

---

## Utilities (`src/utils/`)

### `index.js`

| Export | Purpose |
|---|---|
| `useProfileUser()` | Hook — fetches logged-in user with address, profile, documents |
| `getPersonalFields(user)` | Returns non-empty personal field pairs for display |
| `getAddressBlocks(addresses)` | Groups addresses by type, deduplicates |
| `getDocumentBlocks(documents)` | Maps documents to display-ready objects |
| `toGenderOption(gender)` | Normalizes gender string/object to Dropdown option shape |
| `fetchThumbnail(fileStoreId, tenantId)` | Fetches thumbnail URL from filestore |
| `getFamilyMembers()` | Reads staged family members from `sessionStorage` |
| `addFamilyMember(member)` | Appends a member (prevents duplicates) |
| `setFamilyMembers(list)` | Overwrites the staged list |
| `clearFamilyMembers()` | Clears staged list |
| `stageVerifiedMember(memberId, rel)` | Stages Aadhaar-verified member with relationship |
| `getVerifiedMember()` | Reads single verified member cache |
| `openDocument(fileOrId, stateId)` | Fetches and opens a document in a new tab |
| `TAX_VALIDATORS` | PAN / TAN / GST regex + placeholder + maxLength |
| `FormField` | Label + input + error wrapper component |
| `AvatarUpload` | Avatar with camera button |
| `ProfilePhoto` | Photo or initials fallback |
| `FieldGrid` | Grid of label-value pairs |
| `Section` | Titled section wrapper |
| `PDFSvg` | PDF icon SVG |

### `constants.js`
- `PROFILE_ROUTES` — all citizen route paths
- `AADHAAR_LENGTH` (12), `FAMILY_ID_LENGTH` (13), `OTP_LENGTH` (6)
- `DEFAULT_CASTE` (`"General"`), `SPECIALLY_ABLED_YES` (`"Yes"`)

### `payloads.js`

| Builder | Used by |
|---|---|
| `buildUpdateUserPayload` | `EditProfileForm`, `CreateProfile` |
| `buildSessionUser` | Updates localStorage session after profile save |
| `buildCreateFamilyPayload` | `FamilyMemberAdded` — builds full family creation API body |
| `buildVerifiedMemberInfo` | `AadhaarVerification` — normalizes OTP verify response |
| `buildUpdateDocumentPayload` | `EditDocuments` |
| `buildCreateDocumentPayload` | `EditDocuments` |

### `profileValidators.js`
- `validators` — field-level validators (name, email, altMobileNumber)
- `validateProfile(form, isEmployee)` — full form validation on save
- `hasErrors(errors)` — checks if any error key has a truthy value

---

## Session Storage Keys

| Key | Purpose |
|---|---|
| `Family_Details` | Per-member form data for the Create Family flow |
| `Digit.family.members` | Staged family members list |
| `Digit.family.verifiedMember` | Single Aadhaar-verified member cache |
| `Digit.family.aadhaarLast4` | Last 4 digits of verified Aadhaar |
| `Digit.family.maskedMobile` | Masked mobile of verified member |

---

## MDMS Dependencies

| Module | Master | Used for |
|---|---|---|
| `FamilyCreateType` | `FamilyOptions` | Family dropdown options in ProfileDetails |
| `common-masters` | `GenderType` | Gender dropdown options |
| `PetService` | `CommonFieldsConfig` | Placeholder — to be replaced with Profile MDMS |

---

## Known TODOs

- `TAX_ID_OPTIONS` (PAN/TAN/GST) and `OCCUPATION_OPTIONS` in `IncomeDetails.js` are hardcoded — move to MDMS.
- `PetService → CommonFieldsConfig` MDMS reference in `FamilyCreate` is a placeholder — needs a dedicated Profile MDMS master.
- `buildCreateFamilyPayload` has a `console.log` that should be removed before production.
- `DocumentDetails.js` step is defined but not wired into `createfamilyconfig.js` — flow currently ends at `VehicleDetails`.
- Gram Panchayat, Village, Ward, and Property Usage dropdowns in `RuralForm.js` have placeholder options only — need to be populated from MDMS.

---

## License

UPYOG Source Code is open source under [UPYOG CODE, COPYRIGHT AND CONTRIBUTION LICENSE TERMS](https://upyog.niua.org/employee/Upyog%20Code%20and%20Copyright%20License_v1.pdf)

© 2024 National Institute of Urban Affairs (NIUA)
