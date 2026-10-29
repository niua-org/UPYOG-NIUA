package org.egov.tl.validator;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tl.repository.ServiceRequestRepository;
import org.egov.tl.util.TradeUtil;
import org.egov.tl.web.models.TradeLicense;
import org.egov.tl.web.models.TradeLicenseRequest;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyValidatorTest {

    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private TradeUtil util;

    @InjectMocks
    private PropertyValidator propertyValidator;

    @Test
    void validateProperty_whenPropertyIdBlank_skipsRemoteCall() {
        TradeLicense license = TradeLicense.builder().tenantId("pb.amritsar").propertyId("").build();
        TradeLicenseRequest request = TradeLicenseRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .licenses(List.of(license))
                .build();

        propertyValidator.validateProperty(request);

        verify(serviceRequestRepository, never()).fetchResult(any(), any());
    }

    @Test
    void validateProperty_whenPropertyFound_passes() {
        when(util.getPropertySearchURL()).thenReturn("http://pt?tenantId={1}&propertyIds={2}");
        Map<String, Object> response = new HashMap<>();
        response.put("Properties", List.of(Map.of("propertyId", "prop-1")));
        when(serviceRequestRepository.fetchResult(any(), any())).thenReturn(response);

        TradeLicense license = TradeLicense.builder()
                .tenantId("pb.amritsar")
                .propertyId("prop-1")
                .build();
        TradeLicenseRequest request = TradeLicenseRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .licenses(List.of(license))
                .build();

        propertyValidator.validateProperty(request);

        verify(serviceRequestRepository).fetchResult(any(), any());
    }

    @Test
    void validateProperty_whenRemoteCallFails_throwsInvalidProperty() {
        when(util.getPropertySearchURL()).thenReturn("http://pt?tenantId={1}&propertyIds={2}");
        when(serviceRequestRepository.fetchResult(any(), any())).thenThrow(new RuntimeException("down"));

        TradeLicense license = TradeLicense.builder()
                .tenantId("pb.amritsar")
                .propertyId("prop-9")
                .build();
        TradeLicenseRequest request = TradeLicenseRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .licenses(List.of(license))
                .build();

        assertThatThrownBy(() -> propertyValidator.validateProperty(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Failed to parse the response from property search");
    }
}
