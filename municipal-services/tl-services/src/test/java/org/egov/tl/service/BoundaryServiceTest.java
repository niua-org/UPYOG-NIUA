package org.egov.tl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tl.config.TLConfiguration;
import org.egov.tl.repository.ServiceRequestRepository;
import org.egov.tl.web.models.Address;
import org.egov.tl.web.models.TradeLicense;
import org.egov.tl.web.models.TradeLicenseDetail;
import org.egov.tl.web.models.TradeLicenseRequest;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class BoundaryServiceTest {

    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private ObjectMapper mapper;
    @Mock
    private TLConfiguration config;

    @InjectMocks
    private BoundaryService boundaryService;

    @Test
    void getAreaType_whenLicensesEmpty_returnsWithoutCall() {
        TradeLicenseRequest request = TradeLicenseRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .licenses(List.of())
                .build();

        boundaryService.getAreaType(request, "REVENUE");
    }

    @Test
    void getAreaType_whenLocalityMissing_throwsInvalidAddress() {
        TradeLicenseDetail detail = TradeLicenseDetail.builder()
                .address(Address.builder().build())
                .build();
        TradeLicense license = TradeLicense.builder().tradeLicenseDetail(detail).build();
        TradeLicenseRequest request = TradeLicenseRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .licenses(List.of(license))
                .build();

        assertThatThrownBy(() -> boundaryService.getAreaType(request, "REVENUE"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("locality cannot be null");
    }
}
