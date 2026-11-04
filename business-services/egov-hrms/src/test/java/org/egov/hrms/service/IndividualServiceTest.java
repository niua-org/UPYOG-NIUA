package org.egov.hrms.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import digit.models.coremodels.AuditDetails;
import org.egov.common.models.individual.Address;
import org.egov.common.models.individual.AddressType;
import org.egov.common.models.individual.Gender;
import org.egov.common.models.individual.Individual;
import org.egov.common.models.individual.IndividualBulkResponse;
import org.egov.common.models.individual.IndividualResponse;
import org.egov.common.models.individual.Name;
import org.egov.common.models.individual.UserDetails;
import org.egov.hrms.config.PropertiesManager;
import org.egov.hrms.repository.RestCallRepository;
import org.egov.hrms.utils.HRMSConstants;
import org.egov.hrms.web.contract.UserRequest;
import org.egov.hrms.web.contract.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IndividualServiceTest {

    @Mock
    private PropertiesManager propertiesManager;
    @Mock
    private RestCallRepository restCallRepository;

    private IndividualService individualService;

    @BeforeEach
    void setUp() {
        individualService = new IndividualService(propertiesManager, restCallRepository);
        when(propertiesManager.getIndividualHost()).thenReturn("http://individual/");
        when(propertiesManager.getIndividualCreateEndpoint()).thenReturn("_create");
        when(propertiesManager.getIndividualUpdateEndpoint()).thenReturn("_update");
        when(propertiesManager.getIndividualSearchEndpoint()).thenReturn("_search");
    }

    @Test
    void createUser_mapsIndividualResponse() {
        Individual individual = sampleIndividual();
        IndividualResponse individualResponse = IndividualResponse.builder().individual(individual).build();
        when(restCallRepository.fetchResult(any(), any(), eq(IndividualResponse.class))).thenReturn(individualResponse);

        UserRequest userRequest = sampleUserRequest();
        UserResponse response = individualService.createUser(userRequest);

        assertThat(response.getUser()).hasSize(1);
        assertThat(response.getUser().get(0).getUuid()).isEqualTo("ind-1");
    }

    @Test
    void updateUser_mapsIndividualResponse() {
        Individual individual = sampleIndividual();
        IndividualResponse individualResponse = IndividualResponse.builder().individual(individual).build();
        when(restCallRepository.fetchResult(any(), any(), eq(IndividualResponse.class))).thenReturn(individualResponse);

        UserResponse response = individualService.updateUser(sampleUserRequest());

        assertThat(response.getUser().get(0).getName()).isEqualTo("Alice");
    }

    @Test
    void getUser_returnsBulkMappedUsers() {
        IndividualBulkResponse bulkResponse = IndividualBulkResponse.builder()
                .individual(Collections.singletonList(sampleIndividual()))
                .build();
        when(restCallRepository.fetchResult(any(), any(), eq(IndividualBulkResponse.class))).thenReturn(bulkResponse);

        Map<String, Object> criteria = new HashMap<>();
        criteria.put(HRMSConstants.HRMS_USER_SEARCH_CRITERA_TENANTID, "pb.amritsar");
        criteria.put(HRMSConstants.HRMS_USER_SEARCH_CRITERA_USERNAME, "EMP1");

        UserResponse response = individualService.getUser(new RequestInfo(), criteria);

        assertThat(response.getUser()).hasSize(1);
    }

    private static UserRequest sampleUserRequest() {
        org.egov.hrms.web.contract.User user = org.egov.hrms.web.contract.User.builder()
                .uuid("ind-1")
                .id(1L)
                .name("Alice")
                .gender(Gender.FEMALE.name())
                .emailId("a@example.com")
                .mobileNumber("9999999999")
                .dob(System.currentTimeMillis())
                .tenantId("pb.amritsar")
                .correspondenceAddress("Line 1")
                .active(true)
                .type("EMPLOYEE")
                .roles(Collections.singletonList(org.egov.hrms.model.Role.builder().code("EMP").name("Employee").build()))
                .build();
        return UserRequest.builder().requestInfo(new RequestInfo()).user(user).build();
    }

    private static Individual sampleIndividual() {
        return Individual.builder()
                .id("ind-1")
                .userId("1")
                .userUuid("usvc-1")
                .name(Name.builder().givenName("Alice").build())
                .gender(Gender.FEMALE)
                .email("a@example.com")
                .mobileNumber("9999999999")
                .dateOfBirth(new Date())
                .tenantId("pb.amritsar")
                .address(Collections.singletonList(Address.builder()
                        .type(AddressType.CORRESPONDENCE)
                        .addressLine1("Line 1")
                        .build()))
                .userDetails(UserDetails.builder()
                        .username("EMP1")
                        .tenantId("pb.amritsar")
                        .roles(Collections.singletonList(org.egov.common.models.core.Role.builder()
                                .code("EMP").name("Employee").tenantId("pb.amritsar").build()))
                        .build())
                .auditDetails(AuditDetails.builder()
                        .createdBy("system")
                        .createdTime(1L)
                        .lastModifiedBy("system")
                        .lastModifiedTime(2L)
                        .build())
                .isSystemUserActive(true)
                .build();
    }
}
