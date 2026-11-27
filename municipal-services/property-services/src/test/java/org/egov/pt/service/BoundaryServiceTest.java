package org.egov.pt.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.pt.models.Address;
import org.egov.pt.models.Locality;
import org.egov.pt.models.Property;
import org.egov.pt.repository.ServiceRequestRepository;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class BoundaryServiceTest {

    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper mapper;

    @InjectMocks
    private BoundaryService boundaryService;

    @BeforeEach
    void injectLocationConfig() {
        ReflectionTestUtils.setField(boundaryService, "locationHost", "http://loc");
        ReflectionTestUtils.setField(boundaryService, "locationContextPath", "/loc");
        ReflectionTestUtils.setField(boundaryService, "locationEndpoint", "/boundary");
    }

    @Test
    void getAreaType_whenAddressNull_throwsInvalidAddress() {
        Property property = Property.builder().tenantId("pb.amritsar").build();

        assertThatThrownBy(() -> boundaryService.getAreaType(property, RequestInfo.builder().build(), "REVENUE"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("address or locality cannot be null");
    }

    @Test
    void getAreaType_whenLocalityNull_throwsInvalidAddress() {
        Property property = Property.builder()
                .tenantId("pb.amritsar")
                .address(Address.builder().build())
                .build();

        assertThatThrownBy(() -> boundaryService.getAreaType(property, RequestInfo.builder().build(), "REVENUE"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("address or locality cannot be null");
    }

    @Test
    void getAreaType_whenPropertyNull_doesNotThrow() {
        boundaryService.getAreaType(null, RequestInfo.builder().build(), "REVENUE");
    }
}
