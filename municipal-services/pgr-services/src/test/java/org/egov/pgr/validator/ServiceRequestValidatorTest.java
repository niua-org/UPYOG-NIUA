package org.egov.pgr.validator;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.pgr.config.PGRConfiguration;
import org.egov.pgr.repository.PGRRepository;
import org.egov.pgr.util.HRMSUtil;
import org.egov.pgr.web.models.Priority;
import org.egov.pgr.web.models.RequestSearchCriteria;
import org.egov.pgr.web.models.Service;
import org.egov.pgr.web.models.ServiceRequest;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceRequestValidatorTest {

    @Mock
    private PGRConfiguration config;
    @Mock
    private PGRRepository repository;
    @Mock
    private HRMSUtil hrmsUtil;

    private ServiceRequestValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ServiceRequestValidator(config, repository, hrmsUtil);
        when(config.getAllowedSource()).thenReturn("web,mobile");
        when(config.getAllowedCitizenSearchParameters()).thenReturn("tenantId,serviceCode");
        when(config.getAllowedEmployeeSearchParameters()).thenReturn("tenantId,serviceCode,ids");
        when(config.getStateLevelTenantIdLength()).thenReturn(1);
    }

    @Test
    @DisplayName("validateCreate_whenUnknownServiceCode_throwsInvalidServiceCode")
    void validateCreate_whenUnknownServiceCode_throwsInvalidServiceCode() {
        Service service = Service.builder()
                .serviceCode("UnknownService")
                .priority(Priority.HIGH)
                .source("web")
                .tenantId("pg.citya")
                .address(org.egov.pgr.web.models.Address.builder().build())
                .build();
        ServiceRequest request = ServiceRequest.builder()
                .requestInfo(citizenRequestInfo())
                .service(service)
                .build();

        assertThatThrownBy(() -> validator.validateCreate(request, mdmsFixture()))
                .isInstanceOf(CustomException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("validateCreate_whenServiceCodeBlank_throwsCustomException")
    void validateCreate_whenServiceCodeBlank_throwsCustomException(String serviceCode) {
        Service service = Service.builder()
                .serviceCode(serviceCode)
                .priority(Priority.HIGH)
                .source("web")
                .tenantId("pg.citya")
                .build();
        ServiceRequest request = ServiceRequest.builder()
                .requestInfo(citizenRequestInfo())
                .service(service)
                .build();

        assertThatThrownBy(() -> validator.validateCreate(request, mdmsFixture()))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("validateSearch_whenTenantMissingWithMobile_throwsInvalidSearch")
    void validateSearch_whenTenantMissingWithMobile_throwsInvalidSearch() {
        RequestSearchCriteria criteria = RequestSearchCriteria.builder()
                .mobileNumber("9999999999")
                .build();

        assertThatThrownBy(() -> validator.validateSearch(citizenRequestInfo(), criteria))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("TenantId is mandatory");
    }

    @Test
    @DisplayName("validatePlainSearch_whenTenantIdsEmpty_throwsCustomException")
    void validatePlainSearch_whenTenantIdsEmpty_throwsCustomException() {
        RequestSearchCriteria criteria = RequestSearchCriteria.builder().build();

        assertThatThrownBy(() -> validator.validatePlainSearch(criteria))
                .isInstanceOf(CustomException.class);
    }

    private RequestInfo citizenRequestInfo() {
        User user = User.builder().type("CITIZEN").uuid("citizen-1").tenantId("pg.citya").build();
        return RequestInfo.builder().userInfo(user).build();
    }

    private Object mdmsFixture() {
        return """
                {
                  "MdmsRes": {
                    "RAINMAKER-PGR": {
                      "ServiceDefs": [
                        { "serviceCode": "NoStreetlight" }
                      ]
                    }
                  }
                }
                """;
    }
}
