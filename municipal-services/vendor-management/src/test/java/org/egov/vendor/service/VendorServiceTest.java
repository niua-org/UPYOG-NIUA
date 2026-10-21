package org.egov.vendor.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.tracer.model.CustomException;
import org.egov.vendor.config.VendorConfiguration;
import org.egov.vendor.kafka.Producer;
import org.egov.vendor.repository.AddressRepository;
import org.egov.vendor.repository.VendorAdditionalDetailsRepository;
import org.egov.vendor.repository.VendorRepository;
import org.egov.vendor.utils.MdmsUtil;
import org.egov.vendor.utils.VendorErrorConstants;
import org.egov.vendor.utils.VendorValidator;
import org.egov.vendor.web.models.VendorAdditionalDetails;
import org.egov.vendor.web.models.VendorAdditionalDetailsRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorServiceTest {

    @Mock
    private VendorAdditionalDetailsRepository vendorAdditionalDetailsRepository;
    @Mock
    private KafkaTemplate<String, VendorAdditionalDetails> kafkaTemplate;
    @Mock
    private Producer producer;
    @Mock
    private VendorConfiguration config;
    @Mock
    private VendorValidator vendorValidator;
    @Mock
    private EnrichmentService enrichmentService;
    @Mock
    private VendorRepository vendorRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private MdmsUtil mdmsUtil;

    private VendorService vendorService;

    @BeforeEach
    void setUp() {
        vendorService = new VendorService(
                vendorAdditionalDetailsRepository,
                kafkaTemplate,
                producer,
                config,
                vendorValidator,
                enrichmentService,
                vendorRepository,
                enrichmentService,
                vendorRepository,
                addressRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(vendorService, "util", mdmsUtil);
    }

    @Test
    @DisplayName("saveVendor_whenStateLevelTenant_throwsInvalidTenant")
    void saveVendor_whenStateLevelTenant_throwsInvalidTenant() {
        VendorAdditionalDetailsRequest request = stateLevelCreateRequest();

        assertThatThrownBy(() -> vendorService.saveVendor(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(VendorErrorConstants.INVALID_TENANT);
    }

    @Test
    @DisplayName("saveVendor_whenCityTenant_pushesKafkaMessage")
    void saveVendor_whenCityTenant_pushesKafkaMessage() {
        VendorAdditionalDetailsRequest request = cityLevelCreateRequest();
        when(mdmsUtil.mDMSCall(any(), eq("pg"))).thenReturn("{}");
        when(config.getSaveVendorAdditionalDetails()).thenReturn("save-vendor-topic");

        VendorAdditionalDetails result = vendorService.saveVendor(request);

        assertThat(result).isNotNull();
        ArgumentCaptor<VendorAdditionalDetailsRequest> captor = ArgumentCaptor.forClass(VendorAdditionalDetailsRequest.class);
        verify(producer).push(eq("save-vendor-topic"), captor.capture());
        verify(vendorValidator).validateCreate(eq(request), any());
    }

    @Test
    @DisplayName("searchVendorsAndDetails_whenMissingCriteria_throwsIllegalArgumentException")
    void searchVendorsAndDetails_whenMissingCriteria_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> vendorService.searchVendorsAndDetails(null, RequestInfo.builder().build()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private VendorAdditionalDetailsRequest stateLevelCreateRequest() {
        User user = User.builder().tenantId("pg").uuid("u1").build();
        RequestInfo requestInfo = RequestInfo.builder().userInfo(user).build();
        VendorAdditionalDetails details = VendorAdditionalDetails.builder().tenantId("pg").build();
        return VendorAdditionalDetailsRequest.builder()
                .requestInfo(requestInfo)
                .vendorAdditionalDetails(details)
                .build();
    }

    private VendorAdditionalDetailsRequest cityLevelCreateRequest() {
        User user = User.builder().tenantId("pg.citya").uuid("u1").build();
        RequestInfo requestInfo = RequestInfo.builder().userInfo(user).build();
        VendorAdditionalDetails details = VendorAdditionalDetails.builder().tenantId("pg.citya").build();
        return VendorAdditionalDetailsRequest.builder()
                .requestInfo(requestInfo)
                .vendorAdditionalDetails(details)
                .build();
    }
}
