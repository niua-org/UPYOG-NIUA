package org.egov.user.domain.service.profile;

import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.Address;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.domain.model.enums.AddressType;
import org.egov.user.domain.model.enums.Gender;
import org.egov.user.domain.service.profile.validator.ProfileValidatorImpl;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.web.contract.UserRequest;
import org.egov.user.web.contract.profile.ProfileCreateRequest;
import org.egov.user.web.contract.profile.ProfileUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests validating Section 8 business rules:
 * - Profile Creation: Enforcement of all 23 items (Mandatory Demographics, Address, Document Proofs).
 * - Profile Update: Enforcement of Non-Editable vs Editable fields across Demographics and Address.
 */
public class ProfileSection8ValidationTest {

    private ProfileValidatorImpl profileValidator;

    @BeforeEach
    public void setUp() {
        profileValidator = new ProfileValidatorImpl();
    }

    private Date createDate(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, day, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    @Nested
    @DisplayName("1. User Demographics - Mandatory & Non-Editable Validations")
    class UserDemographicsValidationTests {

        private User createValidUser() {
            return User.builder()
                    .name("Aarav Sharma")
                    .fatherName("Rajesh Sharma")
                    .motherName("Sunita Sharma")
                    .spouseName("Pooja Sharma")
                    .guardian("Rajesh Sharma")
                    .mobileNumber("9876543210")
                    .alternateMobileNumber("9123456789")
                    .dob(createDate(1990, 8, 15))
                    .emailId("aarav.sharma@example.com")
                    .gender(Gender.MALE)
                    .photo("filestore-photo-123")
                    .build();
        }

        @Test
        @DisplayName("validateMandatoryProfileFields should succeed when all mandatory fields are present")
        public void shouldSucceedWhenAllMandatoryDemographicsPresent() {
            User user = createValidUser();
            assertDoesNotThrow(user::validateMandatoryProfileFields);
        }

        @Test
        @DisplayName("validateMandatoryProfileFields should fail when Name is missing")
        public void shouldFailWhenNameMissing() {
            User user = createValidUser();
            user.setName(null);
            CustomException ex = assertThrows(CustomException.class, user::validateMandatoryProfileFields);
            assertEquals(UserConstants.UG_USER_PROFILE_NAME_MANDATORY, ex.getCode());
            assertTrue(ex.getMessage().contains("Name"));
        }

        @Test
        @DisplayName("validateMandatoryProfileFields should fail when Father's Name is missing")
        public void shouldFailWhenFatherNameMissing() {
            User user = createValidUser();
            user.setFatherName("");
            CustomException ex = assertThrows(CustomException.class, user::validateMandatoryProfileFields);
            assertEquals(UserConstants.UG_USER_PROFILE_FATHER_NAME_MANDATORY, ex.getCode());
            assertTrue(ex.getMessage().contains("Father's Name"));
        }

        @Test
        @DisplayName("validateMandatoryProfileFields should fail when Mother's Name is missing")
        public void shouldFailWhenMotherNameMissing() {
            User user = createValidUser();
            user.setMotherName(null);
            CustomException ex = assertThrows(CustomException.class, user::validateMandatoryProfileFields);
            assertEquals(UserConstants.UG_USER_PROFILE_MOTHER_NAME_MANDATORY, ex.getCode());
            assertTrue(ex.getMessage().contains("Mother's Name"));
        }

        @Test
        @DisplayName("validateMandatoryProfileFields should fail when Mobile Number is missing")
        public void shouldFailWhenMobileNumberMissing() {
            User user = createValidUser();
            user.setMobileNumber("   ");
            CustomException ex = assertThrows(CustomException.class, user::validateMandatoryProfileFields);
            assertEquals(UserConstants.UG_USER_PROFILE_MOBILE_MANDATORY, ex.getCode());
            assertTrue(ex.getMessage().contains("Mobile Number"));
        }

        @Test
        @DisplayName("validateMandatoryProfileFields should fail when DOB is missing")
        public void shouldFailWhenDobMissing() {
            User user = createValidUser();
            user.setDob(null);
            CustomException ex = assertThrows(CustomException.class, user::validateMandatoryProfileFields);
            assertEquals(UserConstants.UG_USER_PROFILE_DOB_MANDATORY, ex.getCode());
            assertTrue(ex.getMessage().contains("Date of Birth"));
        }

        @Test
        @DisplayName("validateMandatoryProfileFields should fail when Email ID is missing")
        public void shouldFailWhenEmailIdMissing() {
            User user = createValidUser();
            user.setEmailId(null);
            CustomException ex = assertThrows(CustomException.class, user::validateMandatoryProfileFields);
            assertEquals(UserConstants.UG_USER_PROFILE_EMAIL_MANDATORY, ex.getCode());
            assertTrue(ex.getMessage().contains("Email ID"));
        }

        @Test
        @DisplayName("validateMandatoryProfileFields should fail when Gender is missing")
        public void shouldFailWhenGenderMissing() {
            User user = createValidUser();
            user.setGender(null);
            CustomException ex = assertThrows(CustomException.class, user::validateMandatoryProfileFields);
            assertEquals(UserConstants.UG_USER_PROFILE_GENDER_MANDATORY, ex.getCode());
            assertTrue(ex.getMessage().contains("Gender"));
        }

        @Test
        @DisplayName("validateNonEditableProfileFields should reject modification of Mobile Number")
        public void shouldRejectMobileNumberModification() {
            User existing = createValidUser();
            User incoming = createValidUser();
            incoming.setMobileNumber("9999999999");

            CustomException ex = assertThrows(CustomException.class,
                    () -> incoming.validateNonEditableProfileFields(existing, false));
            assertEquals(UserConstants.UG_USER_PROFILE_NON_EDITABLE_MOBILE, ex.getCode());
            assertTrue(ex.getMessage().contains("Mobile Number"));
        }

        @Test
        @DisplayName("validateNonEditableProfileFields should reject modification of Father's Name")
        public void shouldRejectFatherNameModification() {
            User existing = createValidUser();
            User incoming = createValidUser();
            incoming.setFatherName("Vikram Sharma");

            CustomException ex = assertThrows(CustomException.class,
                    () -> incoming.validateNonEditableProfileFields(existing, false));
            assertEquals(UserConstants.UG_USER_PROFILE_NON_EDITABLE_FATHER_NAME, ex.getCode());
            assertTrue(ex.getMessage().contains("Father's Name"));
        }

        @Test
        @DisplayName("validateNonEditableProfileFields should reject modification of Mother's Name")
        public void shouldRejectMotherNameModification() {
            User existing = createValidUser();
            User incoming = createValidUser();
            incoming.setMotherName("Anita Sharma");

            CustomException ex = assertThrows(CustomException.class,
                    () -> incoming.validateNonEditableProfileFields(existing, false));
            assertEquals(UserConstants.UG_USER_PROFILE_NON_EDITABLE_MOTHER_NAME, ex.getCode());
            assertTrue(ex.getMessage().contains("Mother's Name"));
        }

        @Test
        @DisplayName("validateNonEditableProfileFields should reject modification of Email ID")
        public void shouldRejectEmailIdModification() {
            User existing = createValidUser();
            User incoming = createValidUser();
            incoming.setEmailId("new.email@example.com");

            CustomException ex = assertThrows(CustomException.class,
                    () -> incoming.validateNonEditableProfileFields(existing, false));
            assertEquals(UserConstants.UG_USER_PROFILE_NON_EDITABLE_EMAIL, ex.getCode());
            assertTrue(ex.getMessage().contains("Email ID"));
        }

        @Test
        @DisplayName("validateNonEditableProfileFields should reject Name modification without Aadhaar re-sync")
        public void shouldRejectNameModificationWithoutResync() {
            User existing = createValidUser();
            User incoming = createValidUser();
            incoming.setName("Aarav K. Sharma");

            CustomException ex = assertThrows(CustomException.class,
                    () -> incoming.validateNonEditableProfileFields(existing, false));
            assertEquals(UserConstants.UG_USER_PROFILE_NON_EDITABLE_NAME, ex.getCode());
            assertTrue(ex.getMessage().contains("Name"));
        }

        @Test
        @DisplayName("validateNonEditableProfileFields should allow Name modification with Aadhaar re-sync")
        public void shouldAllowNameModificationWithResync() {
            User existing = createValidUser();
            User incoming = createValidUser();
            incoming.setName("Aarav K. Sharma");

            assertDoesNotThrow(() -> incoming.validateNonEditableProfileFields(existing, true));
        }

        @Test
        @DisplayName("validateNonEditableProfileFields should allow modifying editable fields: Spouse Name, Guardian, Alternate Mobile, Photo")
        public void shouldAllowModifyingEditableDemographicFields() {
            User existing = createValidUser();
            User incoming = createValidUser();
            incoming.setSpouseName("Pooja A. Sharma");
            incoming.setGuardian("Grandfather Name");
            incoming.setAlternateMobileNumber("9888877777");
            incoming.setPhoto("new-filestore-photo-456");

            assertDoesNotThrow(() -> incoming.validateNonEditableProfileFields(existing, false));
        }
    }

    @Nested
    @DisplayName("2. Address - Mandatory & Non-Editable Validations")
    class AddressValidationTests {

        private Address createValidAddress() {
            return Address.builder()
                    .city("Amritsar")
                    .locality("Model Town")
                    .streetName("Mall Road")
                    .houseNumber("Flat 402")
                    .houseName("Green View Residency")
                    .landmark("Near Golden Temple")
                    .pinCode("143001")
                    .latitude(31.6340)
                    .longitude(74.8723)
                    .type(AddressType.CORRESPONDENCE)
                    .addressType("CORRESPONDENCE")
                    .build();
        }

        @Test
        @DisplayName("validateMandatoryAddressFields should succeed when all mandatory fields are present")
        public void shouldSucceedWhenAllMandatoryAddressFieldsPresent() {
            Address address = createValidAddress();
            assertDoesNotThrow(address::validateMandatoryAddressFields);
        }

        @Test
        @DisplayName("validateMandatoryAddressFields should fail when City is missing")
        public void shouldFailWhenCityMissing() {
            Address address = createValidAddress();
            address.setCity(null);
            CustomException ex = assertThrows(CustomException.class, address::validateMandatoryAddressFields);
            assertEquals(UserConstants.UG_USER_ADDRESS_CITY_MANDATORY, ex.getCode());
            assertTrue(ex.getMessage().contains("City"));
        }

        @Test
        @DisplayName("validateMandatoryAddressFields should fail when Locality is missing")
        public void shouldFailWhenLocalityMissing() {
            Address address = createValidAddress();
            address.setLocality("");
            CustomException ex = assertThrows(CustomException.class, address::validateMandatoryAddressFields);
            assertEquals(UserConstants.UG_USER_ADDRESS_LOCALITY_MANDATORY, ex.getCode());
            assertTrue(ex.getMessage().contains("Locality"));
        }

        @Test
        @DisplayName("validateMandatoryAddressFields should fail when Street Name is missing")
        public void shouldFailWhenStreetNameMissing() {
            Address address = createValidAddress();
            address.setStreetName("   ");
            CustomException ex = assertThrows(CustomException.class, address::validateMandatoryAddressFields);
            assertEquals(UserConstants.UG_USER_ADDRESS_STREET_NAME_MANDATORY, ex.getCode());
            assertTrue(ex.getMessage().contains("Street Name"));
        }

        @Test
        @DisplayName("validateMandatoryAddressFields should fail when House Number is missing")
        public void shouldFailWhenHouseNumberMissing() {
            Address address = createValidAddress();
            address.setHouseNumber(null);
            CustomException ex = assertThrows(CustomException.class, address::validateMandatoryAddressFields);
            assertEquals(UserConstants.UG_USER_ADDRESS_HOUSE_NUMBER_MANDATORY, ex.getCode());
            assertTrue(ex.getMessage().contains("House Number"));
        }

        @Test
        @DisplayName("validateMandatoryAddressFields should fail when Landmark is missing")
        public void shouldFailWhenLandmarkMissing() {
            Address address = createValidAddress();
            address.setLandmark(null);
            CustomException ex = assertThrows(CustomException.class, address::validateMandatoryAddressFields);
            assertEquals(UserConstants.UG_USER_ADDRESS_LANDMARK_MANDATORY, ex.getCode());
            assertTrue(ex.getMessage().contains("Landmark"));
        }

        @Test
        @DisplayName("validateMandatoryAddressFields should fail when Pin Code is missing")
        public void shouldFailWhenPinCodeMissing() {
            Address address = createValidAddress();
            address.setPinCode("");
            CustomException ex = assertThrows(CustomException.class, address::validateMandatoryAddressFields);
            assertEquals(UserConstants.UG_USER_ADDRESS_PIN_CODE_MANDATORY, ex.getCode());
            assertTrue(ex.getMessage().contains("Pin Code"));
        }

        @Test
        @DisplayName("validateMandatoryAddressFields should fail when Address Type is missing")
        public void shouldFailWhenAddressTypeMissing() {
            Address address = createValidAddress();
            address.setType(null);
            address.setAddressType(null);
            CustomException ex = assertThrows(CustomException.class, address::validateMandatoryAddressFields);
            assertEquals(UserConstants.UG_USER_ADDRESS_TYPE_MANDATORY, ex.getCode());
            assertTrue(ex.getMessage().contains("Address Type"));
        }

        @Test
        @DisplayName("validateNonEditableAddressFields should reject City modification")
        public void shouldRejectCityModification() {
            Address existing = createValidAddress();
            Address incoming = createValidAddress();
            incoming.setCity("Ludhiana");

            CustomException ex = assertThrows(CustomException.class,
                    () -> incoming.validateNonEditableAddressFields(existing));
            assertEquals(UserConstants.UG_USER_ADDRESS_NON_EDITABLE_CITY, ex.getCode());
            assertTrue(ex.getMessage().contains("City"));
        }

        @Test
        @DisplayName("validateNonEditableAddressFields should reject Locality modification")
        public void shouldRejectLocalityModification() {
            Address existing = createValidAddress();
            Address incoming = createValidAddress();
            incoming.setLocality("Civil Lines");

            CustomException ex = assertThrows(CustomException.class,
                    () -> incoming.validateNonEditableAddressFields(existing));
            assertEquals(UserConstants.UG_USER_ADDRESS_NON_EDITABLE_LOCALITY, ex.getCode());
            assertTrue(ex.getMessage().contains("Locality"));
        }

        @Test
        @DisplayName("validateNonEditableAddressFields should reject House Number modification")
        public void shouldRejectHouseNumberModification() {
            Address existing = createValidAddress();
            Address incoming = createValidAddress();
            incoming.setHouseNumber("Plot 101");

            CustomException ex = assertThrows(CustomException.class,
                    () -> incoming.validateNonEditableAddressFields(existing));
            assertEquals(UserConstants.UG_USER_ADDRESS_NON_EDITABLE_HOUSE_NUMBER, ex.getCode());
            assertTrue(ex.getMessage().contains("House Number"));
        }

        @Test
        @DisplayName("validateNonEditableAddressFields should reject Pin Code modification")
        public void shouldRejectPinCodeModification() {
            Address existing = createValidAddress();
            Address incoming = createValidAddress();
            incoming.setPinCode("143002");

            CustomException ex = assertThrows(CustomException.class,
                    () -> incoming.validateNonEditableAddressFields(existing));
            assertEquals(UserConstants.UG_USER_ADDRESS_NON_EDITABLE_PIN_CODE, ex.getCode());
            assertTrue(ex.getMessage().contains("Pin Code"));
        }

        @Test
        @DisplayName("validateNonEditableAddressFields should allow modifying editable fields: Street Name, House Name, Landmark, Lat/Lng, Address Type")
        public void shouldAllowModifyingEditableAddressFields() {
            Address existing = createValidAddress();
            Address incoming = createValidAddress();
            incoming.setStreetName("Lawrence Road");
            incoming.setHouseName("Royal Palace");
            incoming.setLandmark("Opposite City Mall");
            incoming.setLatitude(31.6350);
            incoming.setLongitude(74.8730);
            incoming.setType(AddressType.PERMANENT);
            incoming.setAddressType("PERMANENT");

            assertDoesNotThrow(() -> incoming.validateNonEditableAddressFields(existing));
        }
    }

    @Nested
    @DisplayName("3. Profile Validator - Contract Creation & Update Validations")
    class ProfileValidatorTests {

        private ProfileCreateRequest createValidProfileRequest() {
            Address address = Address.builder()
                    .city("Amritsar")
                    .locality("Model Town")
                    .streetName("Mall Road")
                    .houseNumber("Flat 402")
                    .landmark("Near Golden Temple")
                    .pinCode("143001")
                    .type(AddressType.CORRESPONDENCE)
                    .build();

            return ProfileCreateRequest.builder()
                    .tenantId("pb.amritsar")
                    .name("Aarav Sharma")
                    .fatherName("Rajesh Sharma")
                    .motherName("Sunita Sharma")
                    .mobileNumber("9876543210")
                    .dob(createDate(1990, 8, 15))
                    .emailId("aarav.sharma@example.com")
                    .gender("MALE")
                    .address(address)
                    .addressProofFileStoreId("filestore-addr-123")
                    .identityProofFileStoreId("filestore-ident-123")
                    .dobProofFileStoreId("filestore-dob-123")
                    .build();
        }

        @Test
        @DisplayName("validateCreate should succeed with all mandatory demographics, address, and document proofs")
        public void shouldSucceedWithAllMandatoryFieldsAndProofs() {
            ProfileCreateRequest request = createValidProfileRequest();
            assertDoesNotThrow(() -> profileValidator.validateCreate(request));
        }

        @Test
        @DisplayName("validateCreate should accept document proofs provided via documents list")
        public void shouldAcceptDocumentProofsFromDocumentsList() {
            ProfileCreateRequest request = createValidProfileRequest();
            request.setAddressProofFileStoreId(null);
            request.setIdentityProofFileStoreId(null);
            request.setDobProofFileStoreId(null);

            UserDocument addrDoc = UserDocument.builder().documentType("ADDRESS_PROOF").filestoreId("f-addr").build();
            UserDocument identDoc = UserDocument.builder().documentType("IDENTITY_PROOF").filestoreId("f-ident").build();
            UserDocument dobDoc = UserDocument.builder().documentType("DOB_PROOF").filestoreId("f-dob").build();

            request.setDocuments(Arrays.asList(addrDoc, identDoc, dobDoc));

            assertDoesNotThrow(() -> profileValidator.validateCreate(request));
        }

        @Test
        @DisplayName("validateCreate should fail when Address Proof is missing")
        public void shouldFailWhenAddressProofMissing() {
            ProfileCreateRequest request = createValidProfileRequest();
            request.setAddressProofFileStoreId(null);
            request.setDocuments(Collections.emptyList());

            CustomException ex = assertThrows(CustomException.class, () -> profileValidator.validateCreate(request));
            assertEquals(UserConstants.UG_USER_DOC_ADDRESS_PROOF_MANDATORY, ex.getCode());
            assertTrue(ex.getMessage().contains("Address Proof"));
        }

        @Test
        @DisplayName("validateCreate should fail when Identity Proof is missing")
        public void shouldFailWhenIdentityProofMissing() {
            ProfileCreateRequest request = createValidProfileRequest();
            request.setIdentityProofFileStoreId(null);
            request.setDocuments(Collections.emptyList());

            CustomException ex = assertThrows(CustomException.class, () -> profileValidator.validateCreate(request));
            assertEquals(UserConstants.UG_USER_DOC_IDENTITY_PROOF_MANDATORY, ex.getCode());
            assertTrue(ex.getMessage().contains("Identity Proof"));
        }

        @Test
        @DisplayName("validateCreate should fail when DOB Proof is missing")
        public void shouldFailWhenDobProofMissing() {
            ProfileCreateRequest request = createValidProfileRequest();
            request.setDobProofFileStoreId(null);
            request.setDocuments(Collections.emptyList());

            CustomException ex = assertThrows(CustomException.class, () -> profileValidator.validateCreate(request));
            assertEquals(UserConstants.UG_USER_DOC_DOB_PROOF_MANDATORY, ex.getCode());
            assertTrue(ex.getMessage().contains("D.O.B. Proof"));
        }

        @Test
        @DisplayName("validateUpdate should reject non-editable field modifications in ProfileUpdateRequest")
        public void shouldRejectNonEditableUpdateInValidator() {
            User existing = User.builder()
                    .id(1001L)
                    .uuid("u-1234")
                    .mobileNumber("9876543210")
                    .name("Aarav Sharma")
                    .fatherName("Rajesh Sharma")
                    .motherName("Sunita Sharma")
                    .emailId("aarav.sharma@example.com")
                    .dob(createDate(1990, 8, 15))
                    .gender(Gender.MALE)
                    .build();

            UserRequest incoming = UserRequest.builder()
                    .id(1001L)
                    .uuid("u-1234")
                    .mobileNumber("9999999999") // changed!
                    .build();

            ProfileUpdateRequest updateReq = ProfileUpdateRequest.builder()
                    .user(incoming)
                    .isResync(false)
                    .build();

            CustomException ex = assertThrows(CustomException.class,
                    () -> profileValidator.validateUpdate(updateReq, existing));
            assertEquals(UserConstants.UG_USER_PROFILE_NON_EDITABLE_MOBILE, ex.getCode());
            assertTrue(ex.getMessage().contains("Mobile Number"));
        }
    }
}
