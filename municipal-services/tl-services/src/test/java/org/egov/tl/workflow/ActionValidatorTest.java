package org.egov.tl.workflow;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.tl.config.TLConfiguration;
import org.egov.tl.web.models.Document;
import org.egov.tl.web.models.TradeLicense;
import org.egov.tl.web.models.TradeLicenseDetail;
import org.egov.tl.web.models.TradeLicenseRequest;
import org.egov.tl.web.models.workflow.BusinessService;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.egov.tl.util.TLConstants.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ActionValidatorTest {

    @Mock
    private WorkflowConfig workflowConfig;
    @Mock
    private WorkflowService workflowService;
    @Mock
    private TLConfiguration config;

    @InjectMocks
    private ActionValidator actionValidator;

    @BeforeEach
    void stubExternalWorkflowOff() {
        when(config.getIsExternalWorkFlowEnabled()).thenReturn(false);
    }

    @Test
    void validateCreateRequest_whenApplyWithoutDocuments_throwsInvalidAction() {
        TradeLicense license = tlLicense(ACTION_APPLY, businessService_TL, null);
        TradeLicenseRequest request = requestWith(license);

        assertThatThrownBy(() -> actionValidator.validateCreateRequest(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("INVALID ACTION");
    }

    @Test
    void validateCreateRequest_whenInitiateForTl_passes() {
        TradeLicense license = tlLicense(ACTION_INITIATE, businessService_TL, null);
        actionValidator.validateCreateRequest(requestWith(license));
    }

    @Test
    void validateCreateRequest_whenBpaWrongAction_throws() {
        TradeLicense license = tlLicense(ACTION_APPLY, businessService_BPA, null);
        assertThatThrownBy(() -> actionValidator.validateCreateRequest(requestWith(license)))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("NOWORKFLOW");
    }

    @Test
    void validateCreateRequest_whenBulkMixedApplicationTypes_throws() {
        TradeLicense first = tlLicense(ACTION_INITIATE, businessService_TL, null);
        first.setApplicationType(TradeLicense.ApplicationTypeEnum.NEW);
        TradeLicense second = tlLicense(ACTION_INITIATE, businessService_TL, null);
        second.setApplicationType(TradeLicense.ApplicationTypeEnum.RENEWAL);
        TradeLicenseRequest request = TradeLicenseRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .licenses(List.of(first, second))
                .build();

        assertThatThrownBy(() -> actionValidator.validateCreateRequest(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Application Types");
    }

    @Test
    void validateUpdateRequest_whenCitizenManualExpire_throws() {
        TradeLicense license = tlLicense(ACTION_MANUALLYEXPIRE, businessService_TL, null);
        TradeLicenseRequest request = TradeLicenseRequest.builder()
                .requestInfo(RequestInfo.builder()
                        .userInfo(User.builder().type("CITIZEN").build())
                        .build())
                .licenses(List.of(license))
                .build();

        assertThatThrownBy(() -> actionValidator.validateUpdateRequest(
                        request, BusinessService.builder().build(), List.of()))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Citizen can not manually expire");
    }

    @Test
    void validateUpdateRequest_whenExternalWorkflowAndPayAction_throws() {
        when(config.getIsExternalWorkFlowEnabled()).thenReturn(true);
        TradeLicense license = tlLicense(ACTION_PAY, businessService_TL, null);
        TradeLicenseRequest request = requestWith(license);

        assertThatThrownBy(() -> actionValidator.validateUpdateRequest(
                        request, BusinessService.builder().build(), List.of()))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Pay action cannot perform directly");
    }

    @Test
    void validateUpdateRequest_whenInitiateWithDocuments_throwsInvalidStatus() {
        TradeLicense license = tlLicense(ACTION_INITIATE, businessService_TL, List.of(Document.builder().build()));
        license.setApplicationType(TradeLicense.ApplicationTypeEnum.NEW);
        TradeLicenseRequest request = requestWith(license);

        assertThatThrownBy(() -> actionValidator.validateUpdateRequest(
                        request, BusinessService.builder().build(), List.of()))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Status cannot be INITIATE");
    }

    @Test
    void validateUpdateRequest_whenStateNotUpdatableAndMissingIds_throwsInvalidUpdate() {
        when(workflowService.isStateUpdatable(eq("APPLIED"), any())).thenReturn(false);
        TradeLicense license = tlLicense("APPROVE", businessService_TL, null);
        license.setId(null);
        license.setStatus("APPLIED");
        license.getTradeLicenseDetail().setOwners(new java.util.ArrayList<>());
        license.getTradeLicenseDetail().setTradeUnits(new java.util.ArrayList<>());
        TradeLicenseRequest request = requestWith(license);

        assertThatThrownBy(() -> actionValidator.validateUpdateRequest(
                        request, BusinessService.builder().build(), List.of()))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("INVALID UPDATE");
    }

    private static TradeLicenseRequest requestWith(TradeLicense license) {
        return TradeLicenseRequest.builder()
                .requestInfo(RequestInfo.builder()
                        .userInfo(User.builder().type("EMPLOYEE").build())
                        .build())
                .licenses(List.of(license))
                .build();
    }

    private static TradeLicense tlLicense(String action, String businessService, List<Document> applicationDocuments) {
        TradeLicenseDetail detail = TradeLicenseDetail.builder()
                .applicationDocuments(applicationDocuments)
                .build();
        return TradeLicense.builder()
                .action(action)
                .businessService(businessService)
                .tradeLicenseDetail(detail)
                .build();
    }
}
