package org.egov.pt.validator;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.common.utils.MultiStateInstanceUtil;
import org.egov.pt.config.PropertyConfiguration;
import org.egov.pt.models.OwnerInfo;
import org.egov.pt.models.Property;
import org.egov.pt.models.PropertyCriteria;
import org.egov.pt.util.PropertyUtil;
import org.egov.pt.web.contracts.PropertyRequest;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PropertyValidatorTest {

    @Mock
    private PropertyUtil propertyUtil;
    @Mock
    private PropertyConfiguration configs;
    @Mock
    private MultiStateInstanceUtil centralInstanceUtil;

    @InjectMocks
    private PropertyValidator propertyValidator;

    @BeforeEach
    void stubCentralInstanceOff() {
        when(centralInstanceUtil.getIsEnvironmentCentralInstance()).thenReturn(false);
    }

    @Test
    void validateCreateRequest_whenOwnersEmpty_throwsOwnerInfoError() {
        Property property = Property.builder().owners(new ArrayList<>()).build();
        PropertyRequest request = PropertyRequest.builder().property(property).build();

        assertThatThrownBy(() -> propertyValidator.validateCreateRequest(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Owners cannot be empty");
    }

    @Test
    void validatePropertyCriteria_whenInboxSearchDisabled_throwsInvalidSearch() {
        when(configs.getIsInboxSearchAllowed()).thenReturn(false);
        PropertyCriteria criteria = PropertyCriteria.builder().isInboxSearch(true).tenantId("pb.amritsar").build();
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(User.builder().type("EMPLOYEE").build())
                .build();

        assertThatThrownBy(() -> propertyValidator.validatePropertyCriteria(criteria, requestInfo))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Inbox search has been disabled");
    }

    @Test
    void validatePropertyCriteria_whenOnlyFromDate_throwsBothDatesRequired() {
        when(configs.getIsInboxSearchAllowed()).thenReturn(true);
        when(propertyUtil.isPropertySearchOpen(org.mockito.ArgumentMatchers.any())).thenReturn(false);
        when(configs.getEmployeeSearchParams()).thenReturn("mobileNumber,ids");
        PropertyCriteria criteria = PropertyCriteria.builder()
                .tenantId("pb.amritsar")
                .fromDate(1L)
                .propertyIds(java.util.Set.of("pid-1"))
                .build();
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(User.builder().type("EMPLOYEE").build())
                .build();

        assertThatThrownBy(() -> propertyValidator.validatePropertyCriteria(criteria, requestInfo))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("both fromDate and toDate");
    }

    @Test
    void validatePropertyCriteria_whenEmployeeEmptyCriteriaWithTenant_throwsEmptyCriteriaError() {
        when(configs.getIsInboxSearchAllowed()).thenReturn(true);
        when(propertyUtil.isPropertySearchOpen(org.mockito.ArgumentMatchers.any())).thenReturn(false);
        when(configs.getEmployeeSearchParams()).thenReturn("mobileNumber,ids");
        PropertyCriteria criteria = PropertyCriteria.builder().tenantId("pb.amritsar").build();
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(User.builder().type("EMPLOYEE").build())
                .build();

        assertThatThrownBy(() -> propertyValidator.validatePropertyCriteria(criteria, requestInfo))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("empty Criteria");
    }

    @Test
    void validatePropertyCriteria_whenCitizenEmptyCriteria_setsMobileFromUser() {
        when(configs.getIsInboxSearchAllowed()).thenReturn(true);
        when(propertyUtil.isPropertySearchOpen(org.mockito.ArgumentMatchers.any())).thenReturn(false);
        when(configs.getCitizenSearchParams()).thenReturn("mobileNumber,ids");
        PropertyCriteria criteria = PropertyCriteria.builder().tenantId("pb.amritsar").build();
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(User.builder().type("CITIZEN").mobileNumber("9876543210").build())
                .build();

        propertyValidator.validatePropertyCriteria(criteria, requestInfo);

        org.assertj.core.api.Assertions.assertThat(criteria.getMobileNumber()).isEqualTo("9876543210");
        org.assertj.core.api.Assertions.assertThat(criteria.getIsCitizen()).isTrue();
    }

    @Test
    void validatePropertyCriteria_whenNameSearchNotAllowedForCitizen_throwsInvalidSearch() {
        when(configs.getIsInboxSearchAllowed()).thenReturn(true);
        when(propertyUtil.isPropertySearchOpen(org.mockito.ArgumentMatchers.any())).thenReturn(false);
        when(configs.getCitizenSearchParams()).thenReturn("mobileNumber");
        PropertyCriteria criteria = PropertyCriteria.builder()
                .tenantId("pb.amritsar")
                .name("owner-name")
                .build();
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(User.builder().type("CITIZEN").mobileNumber("9876543210").build())
                .build();

        assertThatThrownBy(() -> propertyValidator.validatePropertyCriteria(criteria, requestInfo))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Search based on name is not available");
    }
}
