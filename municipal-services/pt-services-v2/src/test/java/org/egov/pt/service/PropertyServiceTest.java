package org.egov.pt.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.pt.config.PropertyConfiguration;
import org.egov.pt.producer.Producer;
import org.egov.pt.repository.PropertyRepository;
import org.egov.pt.validator.PropertyValidator;
import org.egov.pt.web.models.PropertyCriteria;
import org.egov.pt.web.models.UserDetailResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyServiceTest {

    @Mock private Producer producer;
    @Mock private PropertyConfiguration config;
    @Mock private PropertyRepository repository;
    @Mock private EnrichmentService enrichmentService;
    @Mock private PropertyValidator propertyValidator;
    @Mock private UserService userService;
    @Mock private CalculationService calculationService;

    @InjectMocks
    private PropertyService propertyService;

    @Test
    void searchProperty_whenUserSearchEmpty_returnsEmpty() {
        PropertyCriteria criteria = new PropertyCriteria();
        criteria.setTenantId("pb.amritsar");
        criteria.setMobileNumber("9999999999");
        UserDetailResponse emptyUsers = new UserDetailResponse(null, java.util.Collections.emptyList());
        org.mockito.Mockito.doNothing().when(propertyValidator).validatePropertyCriteria(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        when(userService.getUser(criteria, RequestInfo.builder().build())).thenReturn(emptyUsers);

        assertThat(propertyService.searchProperty(criteria, RequestInfo.builder().build())).isEmpty();
    }
}
