package org.egov.pgr.validator;

import com.jayway.jsonpath.JsonPath;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.pgr.config.PGRConfiguration;
import org.egov.pgr.repository.PGRRepository;
import org.egov.pgr.util.HRMSUtil;
import org.egov.pgr.web.models.Address;
import org.egov.pgr.web.models.Priority;
import org.egov.pgr.web.models.RequestSearchCriteria;
import org.egov.pgr.web.models.Service;
import org.egov.pgr.web.models.ServiceRequest;
import org.egov.pgr.web.models.ServiceWrapper;
import org.egov.pgr.web.models.Workflow;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
        when(config.getAllowedCitizenSearchParameters()).thenReturn("tenantId,mobileNumber,serviceRequestId,ids,serviceCode,applicationStatus");
        when(config.getAllowedEmployeeSearchParameters()).thenReturn("tenantId,mobileNumber,serviceRequestId,ids,serviceCode,applicationStatus");
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
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("is not present in MDMS");
    }

    @Test
    @DisplayName("validateCreate_whenKnownServiceCodeAndValidSource_doesNotThrow")
    void validateCreate_whenKnownServiceCodeAndValidSource_doesNotThrow() {
        ServiceRequest request = validCreateRequest("NoStreetlight");

        assertThatCode(() -> validator.validateCreate(request, mdmsFixture())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateCreate_whenInvalidSource_throwsInvalidSource")
    void validateCreate_whenInvalidSource_throwsInvalidSource() {
        Service service = Service.builder()
                .serviceCode("NoStreetlight")
                .priority(Priority.HIGH)
                .source("invalid-channel")
                .tenantId("pg.citya")
                .address(Address.builder().build())
                .build();
        ServiceRequest request = ServiceRequest.builder()
                .requestInfo(citizenRequestInfo())
                .service(service)
                .workflow(Workflow.builder().build())
                .build();

        assertThatThrownBy(() -> validator.validateCreate(request, mdmsFixture()))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("is not valid");
    }

    @Test
    @DisplayName("validateCreate_whenEmployeeMissingCitizen_throwsInvalidRequest")
    void validateCreate_whenEmployeeMissingCitizen_throwsInvalidRequest() {
        Service service = Service.builder()
                .serviceCode("NoStreetlight")
                .priority(Priority.HIGH)
                .source("web")
                .tenantId("pg.citya")
                .address(Address.builder().build())
                .build();
        User employee = org.egov.common.contract.request.User.builder().type("EMPLOYEE").uuid("emp-1").tenantId("pg.citya").build();
        ServiceRequest request = ServiceRequest.builder()
                .requestInfo(RequestInfo.builder().userInfo(employee).build())
                .service(service)
                .workflow(Workflow.builder().build())
                .build();

        assertThatThrownBy(() -> validator.validateCreate(request, mdmsFixture()))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Citizen object cannot be null");
    }

    @Test
    @DisplayName("validateUpdate_whenRecordMissing_throwsInvalidUpdate")
    void validateUpdate_whenRecordMissing_throwsInvalidUpdate() {
        Service service = Service.builder()
                .id("missing-id")
                .tenantId("pg.citya")
                .serviceCode("NoStreetlight")
                .source("web")
                .build();
        ServiceRequest request = ServiceRequest.builder()
                .requestInfo(citizenRequestInfo())
                .service(service)
                .workflow(Workflow.builder().action("UPDATE").build())
                .build();
        when(repository.getServiceWrappers(any())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> validator.validateUpdate(request, mdmsFixture()))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("does not exists");
    }

    @Test
    @DisplayName("validateUpdate_whenRecordExists_doesNotThrow")
    void validateUpdate_whenRecordExists_doesNotThrow() {
        Service service = Service.builder()
                .id("id-1")
                .tenantId("pg.citya")
                .serviceCode("NoStreetlight")
                .source("web")
                .build();
        ServiceRequest request = ServiceRequest.builder()
                .requestInfo(citizenRequestInfo())
                .service(service)
                .workflow(Workflow.builder().action("UPDATE").build())
                .build();
        when(repository.getServiceWrappers(any())).thenReturn(List.of(
                ServiceWrapper.builder().service(service).build()));

        assertThatCode(() -> validator.validateUpdate(request, mdmsFixture())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateSearch_whenEmployeeStateLevelTenant_throwsInvalidSearch")
    void validateSearch_whenEmployeeStateLevelTenant_throwsInvalidSearch() {
        User employee = User.builder().type("EMPLOYEE").uuid("e1").tenantId("pg.citya").build();
        RequestSearchCriteria criteria = RequestSearchCriteria.builder()
                .tenantId("pg")
                .mobileNumber("9999999999")
                .build();

        assertThatThrownBy(() -> validator.validateSearch(RequestInfo.builder().userInfo(employee).build(), criteria))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("state level searches");
    }

    @Test
    @DisplayName("validatePlainSearch_whenTenantIdsPresent_doesNotThrow")
    void validatePlainSearch_whenTenantIdsPresent_doesNotThrow() {
        RequestSearchCriteria criteria = RequestSearchCriteria.builder()
                .tenantIds(Collections.singleton("pg.citya"))
                .build();

        assertThatCode(() -> validator.validatePlainSearch(criteria)).doesNotThrowAnyException();
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

    @Test
    @DisplayName("validateCreate_whenMandatoryFieldsMissing_throwsAggregatedErrors")
    void validateCreate_whenMandatoryFieldsMissing_throwsAggregatedErrors() {
        Service service = Service.builder()
                .serviceCode(" ")
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

    private RequestInfo citizenRequestInfo() {
        User user = User.builder().type("CITIZEN").uuid("citizen-1").tenantId("pg.citya").build();
        return RequestInfo.builder().userInfo(user).build();
    }

    private Object mdmsFixture() {
        return JsonPath.parse("""
                {
                  "MdmsRes": {
                    "RAINMAKER-PGR": {
                      "ServiceDefs": [
                        { "serviceCode": "NoStreetlight", "department": "DEPT" }
                      ]
                    }
                  }
                }
                """).json();
    }

    private ServiceRequest validCreateRequest(String serviceCode) {
        Service service = Service.builder()
                .serviceCode(serviceCode)
                .priority(Priority.HIGH)
                .source("web")
                .tenantId("pg.citya")
                .address(Address.builder().build())
                .build();
        return ServiceRequest.builder()
                .requestInfo(citizenRequestInfo())
                .service(service)
                .workflow(Workflow.builder().build())
                .build();
    }
}
