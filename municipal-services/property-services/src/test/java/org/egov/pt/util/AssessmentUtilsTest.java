package org.egov.pt.util;

import org.egov.common.contract.request.RequestInfo;
import org.egov.pt.models.Assessment;
import org.egov.pt.models.Property;
import org.egov.pt.models.PropertyCriteria;
import org.egov.pt.service.PropertyService;
import org.egov.pt.web.contracts.AssessmentRequest;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentUtilsTest {

    @Mock
    private PropertyService propertyService;
    @Mock
    private UnmaskingUtil unmaskingUtil;

    @InjectMocks
    private AssessmentUtils assessmentUtils;

    @Test
    void getPropertyForAssessment_whenSearchEmpty_throwsPropertyNotFound() {
        AssessmentRequest request = AssessmentRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .assessment(Assessment.builder().tenantId("pb.amritsar").propertyId("prop-1").build())
                .build();
        when(propertyService.searchProperty(any(PropertyCriteria.class), any())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> assessmentUtils.getPropertyForAssessment(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void getPropertyForAssessment_whenPropertyFound_unmasksOwners() {
        Property property = Property.builder().propertyId("prop-1").build();
        AssessmentRequest request = AssessmentRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .assessment(Assessment.builder().tenantId("pb.amritsar").propertyId("prop-1").build())
                .build();
        when(propertyService.searchProperty(any(PropertyCriteria.class), any())).thenReturn(List.of(property));

        Property result = assessmentUtils.getPropertyForAssessment(request);

        org.assertj.core.api.Assertions.assertThat(result).isSameAs(property);
        verify(unmaskingUtil).getOwnerDetailsUnmasked(property, request.getRequestInfo());
    }
}
