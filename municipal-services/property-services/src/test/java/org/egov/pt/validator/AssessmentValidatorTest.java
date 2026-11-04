package org.egov.pt.validator;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.pt.config.PropertyConfiguration;
import org.egov.pt.models.Assessment;
import org.egov.pt.models.Property;
import org.egov.pt.repository.AssessmentRepository;
import org.egov.pt.service.PropertyService;
import org.egov.pt.util.AssessmentUtils;
import org.egov.pt.web.contracts.AssessmentRequest;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssessmentValidatorTest {

    @Mock
    private PropertyService propertyService;
    @Mock
    private AssessmentUtils assmtUtils;
    @Mock
    private AssessmentRepository assessmentRepository;
    @Mock
    private PropertyConfiguration config;

    @InjectMocks
    private AssessmentValidator assessmentValidator;

    @Test
    void validateAssessmentCreate_whenRequestInfoNull_throwsMissingRequestInfo() {
        when(config.getIsAssessmentWorkflowEnabled()).thenReturn(false);
        AssessmentRequest request = AssessmentRequest.builder()
                .assessment(Assessment.builder().tenantId("pb.amritsar").build())
                .build();

        assertThatThrownBy(() -> assessmentValidator.validateAssessmentCreate(request, Property.builder().build()))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void validateAssessmentCreate_whenUserInfoMissing_throwsMissingUserInfo() {
        when(config.getIsAssessmentWorkflowEnabled()).thenReturn(false);
        AssessmentRequest request = AssessmentRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .assessment(Assessment.builder().tenantId("pb.amritsar").build())
                .build();

        assertThatThrownBy(() -> assessmentValidator.validateAssessmentCreate(request, Property.builder().build()))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void validateAssessmentCreate_whenUserMissingUuid_throwsMissingRoleUserId() {
        when(config.getIsAssessmentWorkflowEnabled()).thenReturn(false);
        User user = User.builder().type("EMPLOYEE").tenantId("pb.amritsar").build();
        AssessmentRequest request = AssessmentRequest.builder()
                .requestInfo(RequestInfo.builder().userInfo(user).build())
                .assessment(Assessment.builder().tenantId("pb.amritsar").build())
                .build();

        assertThatThrownBy(() -> assessmentValidator.validateAssessmentCreate(request, Property.builder().build()))
                .isInstanceOf(CustomException.class);
    }
}
