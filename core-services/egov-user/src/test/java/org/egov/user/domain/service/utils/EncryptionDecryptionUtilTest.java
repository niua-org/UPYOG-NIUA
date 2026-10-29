package org.egov.user.domain.service.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.encryption.EncryptionService;
import org.egov.encryption.audit.AuditService;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.Address;
import org.egov.user.domain.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EncryptionDecryptionUtilTest {

    @Mock
    private EncryptionService encryptionService;
    @Mock
    private AuditService auditService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private EncryptionDecryptionUtil util;

    @BeforeEach
    void setUp() {
        util = new EncryptionDecryptionUtil(encryptionService);
        ReflectionTestUtils.setField(util, "auditService", auditService);
        ReflectionTestUtils.setField(util, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(util, "stateLevelTenantId", "pb");
        ReflectionTestUtils.setField(util, "abacEnabled", true);
    }

    @Test
    void encryptObject_null_returnsNull() {
        assertThat(util.encryptObject(null, "User", org.egov.user.domain.model.User.class)).isNull();
    }

    @Test
    void encryptObject_success_delegatesToEncryptionService() throws Exception {
        org.egov.user.domain.model.User user = plainUser("uuid-1");
        when(encryptionService.encryptJson(user, "User", "pb")).thenReturn(objectMapper.valueToTree(user));

        org.egov.user.domain.model.User encrypted =
                util.encryptObject(user, "User", org.egov.user.domain.model.User.class);

        assertThat(encrypted.getUuid()).isEqualTo("uuid-1");
        verify(encryptionService).encryptJson(user, "User", "pb");
    }

    @Test
    void encryptObject_failure_wrapsCustomException() throws Exception {
        org.egov.user.domain.model.User user = plainUser("uuid-1");
        doThrow(new RuntimeException("down")).when(encryptionService).encryptJson(any(), any(), any());

        assertThatThrownBy(() -> util.encryptObject(user, "User", org.egov.user.domain.model.User.class))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", "ENCRYPTION_ERROR");
    }

    @Test
    void decryptObject_null_returnsNull() {
        org.egov.user.domain.model.User result = util.decryptObject(null, "User",
                org.egov.user.domain.model.User.class, RequestInfo.builder().build());
        assertThat(result).isNull();
    }

    @Test
    void decryptObject_unencryptedUser_preservesRolesWithoutCallingEncryptionService() {
        org.egov.user.domain.model.User user = plainUser("uuid-1");
        user.setRoles(Set.of(Role.builder().code("CITIZEN").build()));
        RequestInfo requestInfo = requestWithActor("uuid-1");

        org.egov.user.domain.model.User result =
                util.decryptObject(user, "User", org.egov.user.domain.model.User.class, requestInfo);

        assertThat(result).isSameAs(user);
        assertThat(result.getRoles()).hasSize(1);
    }

    @Test
    void decryptObject_encryptedUser_decryptsAndRestoresRoles() throws Exception {
        org.egov.user.domain.model.User encryptedUser = plainUser("uuid-2");
        encryptedUser.setMobileNumber("1|cipher-text");
        encryptedUser.setRoles(Set.of(Role.builder().code("EMPLOYEE").build()));
        org.egov.user.domain.model.User decryptedUser = plainUser("uuid-2");
        decryptedUser.setMobileNumber("9999999999");
        RequestInfo requestInfo = requestWithActor("other");

        when(encryptionService.decryptJson(any(), any(), eq("User"), any(), eq(org.egov.user.domain.model.User.class)))
                .thenReturn(Collections.singletonList(decryptedUser));

        org.egov.user.domain.model.User result = util.decryptObject(encryptedUser, "User",
                org.egov.user.domain.model.User.class, requestInfo);

        assertThat(result.getMobileNumber()).isEqualTo("9999999999");
        assertThat(result.getRoles()).extracting(Role::getCode).containsExactly("EMPLOYEE");
    }

    @Test
    void isUserDecryptingForSelf_matchingUuid_returnsTrue() {
        org.egov.user.domain.model.User user = plainUser("uuid-3");
        User actor = User.builder().uuid("uuid-3").build();

        assertThat(util.isUserDecryptingForSelf(user, actor)).isTrue();
    }

    @Test
    void isUserDecryptingForSelf_addressObject_returnsFalse() {
        assertThat(util.isUserDecryptingForSelf(Address.builder().city("City").build(),
                User.builder().uuid("uuid-3").build())).isFalse();
    }

    @Test
    void getKeyToDecrypt_abacDisabled_usesUserSelfPurpose() {
        ReflectionTestUtils.setField(util, "abacEnabled", false);
        org.egov.user.domain.model.User user = plainUser("uuid-4");

        Map<String, String> keyPurpose = util.getKeyToDecrypt(user, User.builder().uuid("uuid-4").build());

        assertThat(keyPurpose).containsEntry("key", "UserSelf");
        assertThat(keyPurpose).containsEntry("purpose", "AbacDisabled");
    }

    @Test
    void getKeyToDecrypt_addressObject_usesAddressPurpose() {
        Address address = Address.builder().city("City").build();

        Map<String, String> keyPurpose = util.getKeyToDecrypt(address, User.builder().uuid("actor").build());

        assertThat(keyPurpose).containsEntry("key", "Address");
        assertThat(keyPurpose).containsEntry("purpose", "Address");
    }

    @Test
    void getKeyToDecrypt_selfUser_usesSelfPurpose() {
        org.egov.user.domain.model.User user = plainUser("uuid-self");

        Map<String, String> keyPurpose = util.getKeyToDecrypt(user, User.builder().uuid("uuid-self").build());

        assertThat(keyPurpose).containsEntry("key", "UserSelf");
        assertThat(keyPurpose).containsEntry("purpose", "Self");
    }

    @Test
    void getKeyToDecrypt_bulkUserList_usesBulkSearchPurpose() {
        List<org.egov.user.domain.model.User> users = List.of(plainUser("a"), plainUser("b"));

        Map<String, String> keyPurpose = util.getKeyToDecrypt(users, User.builder().uuid("actor").build());

        assertThat(keyPurpose).containsEntry("key", "User");
        assertThat(keyPurpose).containsEntry("purpose", "BulkSearchResult");
    }

    private org.egov.user.domain.model.User plainUser(String uuid) {
        return org.egov.user.domain.model.User.builder()
                .uuid(uuid)
                .username("user")
                .name("User")
                .mobileNumber("9999999999")
                .build();
    }

    private RequestInfo requestWithActor(String uuid) {
        return RequestInfo.builder()
                .userInfo(User.builder().uuid(uuid).type("EMPLOYEE").build())
                .build();
    }
}
