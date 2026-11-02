package org.egov.user.domain.service.profile;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.Address;
import org.egov.user.domain.model.AddressSearchCriteria;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.domain.model.enums.AddressType;
import org.egov.user.domain.service.UserService;
import org.egov.user.domain.service.address.AddressServiceImpl;
import org.egov.user.domain.service.document.UserDocumentServiceImpl;
import org.egov.user.domain.service.multiauth.strategy.AadhaarAuthStrategy;
import org.egov.user.domain.service.multiauth.strategy.MobileAuthStrategy;
import org.egov.user.domain.service.profile.validator.ProfileValidatorImpl;
import org.egov.user.domain.service.utils.EncryptionDecryptionUtil;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.persistence.repository.*;
import org.egov.user.web.contract.AddressRequest;
import org.egov.user.web.contract.document.UserDocumentRequest;
import org.egov.user.web.contract.document.UserDocumentSearchCriteria;
import org.egov.user.web.contract.multiauth.LoginV2Request;
import org.egov.user.web.contract.profile.ProfileResyncRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests verifying security edge cases, single vs multi-instance address rules,
 * SCD Type 2 soft-versioning for addresses and documents, and OTP rate-limiting/lockout.
 */
@ExtendWith(MockitoExtension.class)
public class ProfileResyncAndScdSecurityTest {

    @Nested
    @DisplayName("1. Aadhaar Re-Sync Security Tests")
    class ResyncSecurityTests {

        private ProfileValidatorImpl validator;

        @BeforeEach
        public void setUp() {
            validator = new ProfileValidatorImpl();
        }

        @Test
        @DisplayName("Re-sync succeeds when authenticated user UUID matches target citizen record")
        public void testResync_WhenAuthenticatedUserMatchesTarget_Succeeds() {
            RequestInfo requestInfo = RequestInfo.builder()
                    .userInfo(User.builder().uuid("citizen-uuid-1").build())
                    .build();

            ProfileResyncRequest request = ProfileResyncRequest.builder()
                    .requestInfo(requestInfo)
                    .uuid("citizen-uuid-1")
                    .tenantId("pb.amritsar")
                    .otp("123456")
                    .aadhaarNumber("999988881234")
                    .build();

            org.egov.user.domain.model.User existingUser = org.egov.user.domain.model.User.builder()
                    .uuid("citizen-uuid-1")
                    .aadhaarNumber("XXXXXXXX1234")
                    .tenantId("pg")
                    .build();

            assertDoesNotThrow(() -> validator.validateResync(request, existingUser));
        }

        @Test
        @DisplayName("Re-sync rejected with UNAUTHORIZED_RESYNC when logged-in user is Citizen A but targets Citizen B")
        public void testResync_WhenCitizenATriesToResyncCitizenB_ThrowsUnauthorized() {
            RequestInfo requestInfo = RequestInfo.builder()
                    .userInfo(User.builder().uuid("citizen-A-uuid").build())
                    .build();

            ProfileResyncRequest request = ProfileResyncRequest.builder()
                    .requestInfo(requestInfo)
                    .uuid("citizen-B-uuid")
                    .tenantId("pb.amritsar")
                    .otp("123456")
                    .build();

            org.egov.user.domain.model.User citizenB = org.egov.user.domain.model.User.builder()
                    .uuid("citizen-B-uuid")
                    .tenantId("pg")
                    .build();

            CustomException ex = assertThrows(CustomException.class, () -> validator.validateResync(request, citizenB));
            assertEquals(UserConstants.ERROR_UNAUTHORIZED_RESYNC, ex.getCode());
        }

        @Test
        @DisplayName("Re-sync rejected when Aadhaar number does not match registered citizen Aadhaar")
        public void testResync_WhenAadhaarNumberDoesNotMatchRegistered_ThrowsMismatch() {
            RequestInfo requestInfo = RequestInfo.builder()
                    .userInfo(User.builder().uuid("citizen-uuid-1").build())
                    .build();

            ProfileResyncRequest request = ProfileResyncRequest.builder()
                    .requestInfo(requestInfo)
                    .uuid("citizen-uuid-1")
                    .tenantId("pb.amritsar")
                    .otp("123456")
                    .aadhaarNumber("999988885678") // Ends in 5678
                    .build();

            org.egov.user.domain.model.User existingUser = org.egov.user.domain.model.User.builder()
                    .uuid("citizen-uuid-1")
                    .aadhaarNumber("XXXXXXXX1234") // Ends in 1234
                    .tenantId("pg")
                    .build();

            CustomException ex = assertThrows(CustomException.class, () -> validator.validateResync(request, existingUser));
            assertEquals(UserConstants.ERROR_AADHAAR_CANNOT_BE_CHANGED, ex.getCode());
        }
    }

    @Nested
    @DisplayName("2. Address Rules & SCD Type 2 Versioning Tests")
    class AddressRuleAndScdTests {

        @Mock
        private AddressRepository addressRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private EncryptionDecryptionUtil encryptionDecryptionUtil;

        @Mock
        private FileStoreRepository fileStoreRepository;

        private AddressServiceImpl addressService;

        @BeforeEach
        public void setUp() {
            addressService = new AddressServiceImpl(addressRepository, userRepository, encryptionDecryptionUtil, fileStoreRepository);
            lenient().when(encryptionDecryptionUtil.encryptObject(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
            lenient().when(encryptionDecryptionUtil.decryptObject(any(), any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        @DisplayName("Creating PERMANENT address deactivates existing active PERMANENT address")
        public void testCreateAddress_PermanentDeactivatesExistingPermanent() {
            Address existingPermanent = Address.builder()
                    .id(101L)
                    .userId(1L)
                    .userUuid("user-1")
                    .type(AddressType.PERMANENT)
                    .address("Old Address, Sector 1")
                    .city("Amritsar")
                    .locality("Locality A")
                    .streetName("Street A")
                    .houseNumber("101")
                    .pinCode("143001")
                    .status(UserConstants.ADDRESS_ACTIVE_STATUS)
                    .build();

            Address incomingPermanent = Address.builder()
                    .userId(1L)
                    .type(AddressType.PERMANENT)
                    .address("New Address, Sector 2")
                    .city("Amritsar")
                    .locality("Locality B")
                    .streetName("Street B")
                    .houseNumber("102")
                    .pinCode("143001")
                    .landmark("Near Park")
                    .tenantId("pb.amritsar")
                    .build();

            when(userRepository.getUuidByUserId(1L)).thenReturn("user-1");
            when(userRepository.getTenantIdByUserId(1L)).thenReturn("pb.amritsar");
            when(addressRepository.getAddressV2(any(AddressSearchCriteria.class))).thenReturn(Collections.singletonList(existingPermanent));
            when(addressRepository.createAddressV2(any(Address.class), eq(1L), anyString())).thenReturn(incomingPermanent);

            AddressRequest request = AddressRequest.builder()
                    .address(incomingPermanent)
                    .build();

            addressService.createAddress(request);

            // Verifies existing PERMANENT address is soft-deactivated
            verify(addressRepository, times(1)).updateAddressV2(eq(101L), eq(1L), eq(UserConstants.ADDRESS_INACTIVE_STATUS));
            verify(addressRepository, times(1)).createAddressV2(any(Address.class), eq(1L), anyString());
        }

        @Test
        @DisplayName("Creating CORRESPONDENCE address does NOT deactivate existing active CORRESPONDENCE address")
        public void testCreateAddress_CorrespondenceAllowsMultipleActive() {
            Address existingCorr = Address.builder()
                    .id(102L)
                    .userId(1L)
                    .userUuid("user-1")
                    .type(AddressType.CORRESPONDENCE)
                    .address("Office 1, Cyber City")
                    .city("Amritsar")
                    .locality("Locality A")
                    .streetName("Street A")
                    .houseNumber("201")
                    .pinCode("143001")
                    .status(UserConstants.ADDRESS_ACTIVE_STATUS)
                    .build();

            Address incomingCorr = Address.builder()
                    .userId(1L)
                    .type(AddressType.CORRESPONDENCE)
                    .address("Office 2, Tech Zone")
                    .city("Amritsar")
                    .locality("Locality B")
                    .streetName("Street B")
                    .houseNumber("202")
                    .pinCode("143001")
                    .landmark("Near Metro")
                    .tenantId("pb.amritsar")
                    .build();

            when(userRepository.getUuidByUserId(1L)).thenReturn("user-1");
            when(userRepository.getTenantIdByUserId(1L)).thenReturn("pb.amritsar");
            when(addressRepository.getAddressV2(any(AddressSearchCriteria.class))).thenReturn(Collections.singletonList(existingCorr));
            when(addressRepository.createAddressV2(any(Address.class), eq(1L), anyString())).thenReturn(incomingCorr);

            AddressRequest request = AddressRequest.builder()
                    .address(incomingCorr)
                    .build();

            addressService.createAddress(request);

            // Verifies existing CORRESPONDENCE address is NOT deactivated
            verify(addressRepository, never()).updateAddressV2(anyLong(), anyLong(), anyString());
            verify(addressRepository, times(1)).createAddressV2(any(Address.class), eq(1L), anyString());
        }

        @Test
        @DisplayName("Updating address performs SCD Type 2: soft-deactivates previous record and creates new active entry")
        public void testUpdateAddress_PerformsScdType2() {
            Address existing = Address.builder()
                    .id(205L)
                    .userId(1L)
                    .userUuid("user-1")
                    .type(AddressType.PERMANENT)
                    .city("Amritsar")
                    .locality("Locality A")
                    .streetName("Old Street")
                    .houseNumber("501")
                    .pinCode("143001")
                    .landmark("Old Landmark")
                    .tenantId("pb.amritsar")
                    .version(1L)
                    .status(UserConstants.ADDRESS_ACTIVE_STATUS)
                    .build();

            Address updateIncoming = Address.builder()
                    .id(205L)
                    .userId(1L)
                    .type(AddressType.PERMANENT)
                    .city("Amritsar")
                    .locality("Locality A")
                    .streetName("New Street Name") // Editable
                    .houseNumber("501")
                    .pinCode("143001")
                    .landmark("New Landmark") // Editable
                    .tenantId("pb.amritsar")
                    .build();

            when(addressRepository.getAddressV2(any(AddressSearchCriteria.class))).thenReturn(Collections.singletonList(existing));
            when(userRepository.getTenantIdByUserId(1L)).thenReturn("pb.amritsar");
            when(addressRepository.createAddressV2(any(Address.class), eq(1L), anyString())).thenAnswer(invocation -> {
                Address a = invocation.getArgument(0);
                a.setId(206L);
                return a;
            });

            AddressRequest request = AddressRequest.builder()
                    .address(updateIncoming)
                    .build();

            Address result = addressService.updateAddress(request);

            assertNotNull(result);
            assertEquals(206L, result.getId());
            assertEquals(2L, result.getVersion());
            assertEquals(UserConstants.ADDRESS_ACTIVE_STATUS, result.getStatus());

            // Verifies previous ID 205 is deactivated
            verify(addressRepository, times(1)).updateAddressV2(eq(205L), eq(1L), eq(UserConstants.ADDRESS_INACTIVE_STATUS));
            verify(addressRepository, times(1)).createAddressV2(any(Address.class), eq(1L), anyString());
        }
    }

    @Nested
    @DisplayName("3. Document SCD Type 2 Versioning Tests")
    class DocumentScdTests {

        @Mock
        private UserDocumentRepository documentRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private FileStoreRepository fileStoreRepository;

        private UserDocumentServiceImpl documentService;

        @BeforeEach
        public void setUp() {
            documentService = new UserDocumentServiceImpl(documentRepository, userRepository, fileStoreRepository);
        }

        @Test
        @DisplayName("Updating document performs SCD Type 2: soft-deactivates previous document and inserts new active version")
        public void testUpdateDocument_PerformsScdType2() throws Exception {
            UserDocument existingDoc = UserDocument.builder()
                    .id("doc-uuid-1")
                    .userId(101L)
                    .userUuid("user-uuid-1")
                    .tenantId("pb.amritsar")
                    .documentType("ADDRESS_PROOF")
                    .filestoreId("file-store-old")
                    .active(true)
                    .build();

            UserDocument updateIncoming = UserDocument.builder()
                    .id("doc-uuid-1")
                    .documentType("ADDRESS_PROOF")
                    .filestoreId("file-store-new")
                    .build();

            when(documentRepository.search(any(UserDocumentSearchCriteria.class))).thenReturn(Collections.singletonList(existingDoc));
            when(documentRepository.create(any(UserDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(fileStoreRepository.getUrlByFileStoreId(anyString(), anyList()))
                    .thenReturn(Collections.singletonMap("file-store-new", "http://filestore/download/file-store-new"));

            UserDocumentRequest request = UserDocumentRequest.builder()
                    .document(updateIncoming)
                    .build();

            UserDocument result = documentService.updateDocument(request);

            assertNotNull(result);
            assertNotEquals("doc-uuid-1", result.getId());
            assertEquals(Boolean.TRUE, result.getActive());
            assertEquals("file-store-new", result.getFilestoreId());

            // Verifies old document ID was soft-deleted
            verify(documentRepository, times(1)).delete(eq("doc-uuid-1"), anyLong());
            verify(documentRepository, times(1)).create(any(UserDocument.class));
        }

        @Test
        @DisplayName("Document creation rejected with FILESTORE_NOT_FOUND when filestoreId is invalid")
        public void testCreateDocument_WhenFilestoreNotFound_ThrowsException() throws Exception {
            when(fileStoreRepository.getUrlByFileStoreId(anyString(), anyList()))
                    .thenReturn(Collections.emptyMap());
            when(userRepository.getTenantIdByUserId(anyLong())).thenReturn("pb.amritsar");

            UserDocument doc = UserDocument.builder()
                    .documentType("ADDRESS_PROOF")
                    .filestoreId("invalid-file-id")
                    .build();

            UserDocumentRequest request = UserDocumentRequest.builder()
                    .userId(101L)
                    .documents(Collections.singletonList(doc))
                    .build();

            CustomException ex = assertThrows(CustomException.class, () -> documentService.createDocuments(request));
            assertEquals(UserConstants.ERROR_FILESTORE_NOT_FOUND, ex.getCode());
        }
    }

    @Nested
    @DisplayName("4. OTP Attempt Rate Limiting & Lockout Tests")
    class OtpLockoutTests {

        @Mock
        private UserService userService;

        @Mock
        private UserRepository userRepository;

        @Mock
        private OtpRepository otpRepository;

        @Mock
        private EncryptionDecryptionUtil encryptionDecryptionUtil;

        private MobileAuthStrategy mobileAuthStrategy;

        @BeforeEach
        public void setUp() {
            org.egov.user.config.UserConfiguration userConfig = org.egov.user.config.UserConfiguration.builder()
                    .citizenLoginPasswordOtpFixedEnabled(false)
                    .otpMaxInvalidAttempts(3)
                    .build();
            mobileAuthStrategy = new MobileAuthStrategy(userService, userRepository, otpRepository, encryptionDecryptionUtil, userConfig);
        }

        @Test
        @DisplayName("Mobile OTP locks after 3 invalid attempts and throws MAX_OTP_ATTEMPTS_EXCEEDED")
        public void testMobileOtp_LocksAfter3FailedAttempts() {
            when(otpRepository.validateOtp(any())).thenReturn(false);

            LoginV2Request request = LoginV2Request.builder()
                    .mobileNumber("9876543210")
                    .otp("000000")
                    .tenantId("pb.amritsar")
                    .build();

            // Attempt 1 -> INVALID_OTP
            CustomException ex1 = assertThrows(CustomException.class, () -> mobileAuthStrategy.authenticateAndResolveUser(request));
            assertEquals(UserConstants.ERROR_INVALID_OTP, ex1.getCode());

            // Attempt 2 -> INVALID_OTP
            CustomException ex2 = assertThrows(CustomException.class, () -> mobileAuthStrategy.authenticateAndResolveUser(request));
            assertEquals(UserConstants.ERROR_INVALID_OTP, ex2.getCode());

            // Attempt 3 -> MAX_OTP_ATTEMPTS_EXCEEDED (Lockout triggered)
            CustomException ex3 = assertThrows(CustomException.class, () -> mobileAuthStrategy.authenticateAndResolveUser(request));
            assertEquals(UserConstants.ERROR_MAX_OTP_ATTEMPTS_EXCEEDED, ex3.getCode());

            // Attempt 4 -> Immediately blocked with MAX_OTP_ATTEMPTS_EXCEEDED before checking OTP
            CustomException ex4 = assertThrows(CustomException.class, () -> mobileAuthStrategy.authenticateAndResolveUser(request));
            assertEquals(UserConstants.ERROR_MAX_OTP_ATTEMPTS_EXCEEDED, ex4.getCode());
        }
    }
}
