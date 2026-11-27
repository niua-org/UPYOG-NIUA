package org.egov.user.domain.service.address;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.Address;
import org.egov.user.domain.model.AddressSearchCriteria;
import org.egov.user.domain.model.enums.AddressType;
import org.egov.user.domain.service.utils.EncryptionDecryptionUtil;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.persistence.repository.AddressRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.web.contract.AddressRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock
    private AddressRepository addressRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EncryptionDecryptionUtil encryptionDecryptionUtil;

    private AddressServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        service = new AddressServiceImpl(addressRepository, userRepository, encryptionDecryptionUtil, null);
        lenient().when(encryptionDecryptionUtil.encryptObject(any(), eq(UserConstants.USER_ADDRESS_ENCRYPTION_KEY), eq(Address.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(encryptionDecryptionUtil.decryptObject(any(Address.class), eq(UserConstants.USER_ADDRESS_ENCRYPTION_KEY), eq(Address.class), eq(null)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(encryptionDecryptionUtil.decryptObject(any(List.class), eq(UserConstants.USER_ADDRESS_ENCRYPTION_KEY), eq(Address.class), eq(null)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createAddress_nullRequest_throwsInvalidAddressRequest() {
        assertThatThrownBy(() -> service.createAddress(null))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getCode())
                .isEqualTo(UserConstants.ERROR_INVALID_ADDRESS_REQUEST);
    }

    @Test
    void searchAddress_nullCriteria_throwsInvalidSearchCriteria() {
        assertThatThrownBy(() -> service.searchAddress(null, RequestInfo.builder().build()))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getCode())
                .isEqualTo(UserConstants.ERROR_INVALID_SEARCH_CRITERIA);
    }

    @Test
    void createAddress_validRequest_persistsAndReturnsDecryptedAddress() {
        Address address = validAddress();
        AddressRequest request = AddressRequest.builder()
                .uuid("citizen-uuid")
                .requestInfo(RequestInfo.builder().build())
                .address(address)
                .build();

        when(userRepository.getUserIdByUuid("citizen-uuid")).thenReturn(42L);
        when(userRepository.getTenantIdByUserId(42L)).thenReturn("pb.amritsar");
        when(addressRepository.getAddressV2(any(AddressSearchCriteria.class))).thenReturn(Collections.emptyList());
        when(addressRepository.createAddressV2(any(Address.class), eq(42L), eq("pb.amritsar")))
                .thenAnswer(inv -> {
                    Address saved = inv.getArgument(0);
                    saved.setId(99L);
                    return saved;
                });

        Address result = service.createAddress(request);

        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getUserUuid()).isEqualTo("citizen-uuid");
        assertThat(result.getAddress()).contains("Flat 402");
        verify(addressRepository).createAddressV2(any(Address.class), eq(42L), eq("pb.amritsar"));
    }

    @Test
    void createAddress_duplicateActiveAddress_throwsDuplicateError() {
        Address address = validAddress();
        AddressRequest request = AddressRequest.builder()
                .uuid("citizen-uuid")
                .requestInfo(RequestInfo.builder().build())
                .address(address)
                .build();
        Address existing = validAddress();
        existing.setId(1L);
        existing.setUserId(42L);

        when(userRepository.getUserIdByUuid("citizen-uuid")).thenReturn(42L);
        when(userRepository.getTenantIdByUserId(42L)).thenReturn("pb.amritsar");
        when(addressRepository.getAddressV2(any(AddressSearchCriteria.class))).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.createAddress(request))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getCode())
                .isEqualTo(UserConstants.ERROR_DUPLICATE_ADDRESS);
    }

    @Test
    void createAddress_missingAddressProof_throwsMandatoryError() {
        Address address = validAddress();
        address.setFilestoreId(null);
        AddressRequest request = AddressRequest.builder()
                .uuid("citizen-uuid")
                .requestInfo(RequestInfo.builder().build())
                .address(address)
                .build();

        when(userRepository.getUserIdByUuid("citizen-uuid")).thenReturn(42L);
        when(userRepository.getTenantIdByUserId(42L)).thenReturn("pb.amritsar");

        assertThatThrownBy(() -> service.createAddress(request))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getCode())
                .isEqualTo(UserConstants.UG_USER_ADDRESS_FILESTORE_ID_MANDATORY);
    }

    private static Address validAddress() {
        return Address.builder()
                .city("Amritsar")
                .locality("Model Town")
                .streetName("Mall Road")
                .houseNumber("Flat 402")
                .landmark("Near Golden Temple")
                .pinCode("143001")
                .filestoreId("filestore-addr-proof-001")
                .type(AddressType.CORRESPONDENCE)
                .addressType("CORRESPONDENCE")
                .tenantId("pb.amritsar")
                .build();
    }
}
