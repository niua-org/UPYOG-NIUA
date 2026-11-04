package org.egov.vendor.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.vendor.util.VendorUtil;
import org.egov.vendor.validator.VendorValidator;
import org.egov.vendor.web.model.Vendor;
import org.egov.vendor.web.model.VendorRequest;
import org.egov.vendor.web.model.VendorResponse;
import org.egov.vendor.web.model.VendorSearchCriteria;
import org.egov.vendor.repository.VendorRepository;
import org.egov.vendor.config.VendorConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VendorServiceTest {

    @Mock private VendorUtil util;
    @Mock private VendorRepository vendorRepository;
    @Mock private VendorRepository repository;
    @Mock private VendorValidator vendorValidator;
    @Mock private EnrichmentService enrichmentService;
    @Mock private VendorConfiguration config;

    @InjectMocks
    private VendorService vendorService;

    @Test
    void create_whenStateLevelTenant_throws() {
        VendorRequest request = VendorRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .vendor(Vendor.builder().tenantId("pb").build())
                .build();

        assertThatThrownBy(() -> vendorService.create(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("StateLevel");
    }

    @Test
    void vendorsearch_whenRepositoryEmpty_returnsEmptyVendorList() {
        VendorSearchCriteria criteria = VendorSearchCriteria.builder()
                .tenantId("pb.amritsar")
                .ids(Collections.singletonList("v1"))
                .build();
        VendorResponse empty = new VendorResponse();
        empty.setVendor(Collections.emptyList());
        when(config.getMaxSearchLimit()).thenReturn(100);
        when(config.getDefaultOffset()).thenReturn(0);
        when(repository.getVendorData(criteria)).thenReturn(empty);

        assertThat(vendorService.vendorsearch(criteria, RequestInfo.builder().build()).getVendor()).isEmpty();
    }
}
