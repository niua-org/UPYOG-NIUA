package org.egov.waterconnection.validator;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.common.utils.MultiStateInstanceUtil;
import org.egov.tracer.model.CustomException;
import org.egov.waterconnection.constants.WCConstants;
import org.egov.waterconnection.service.MeterInfoValidator;
import org.egov.waterconnection.service.PropertyValidator;
import org.egov.waterconnection.service.WaterFieldValidator;
import org.egov.waterconnection.util.EncryptionDecryptionUtil;
import org.egov.waterconnection.web.models.Document;
import org.egov.waterconnection.web.models.SearchCriteria;
import org.egov.waterconnection.web.models.ValidatorResult;
import org.egov.waterconnection.web.models.WaterConnection;
import org.egov.waterconnection.web.models.WaterConnectionRequest;
import org.egov.waterconnection.web.models.workflow.ProcessInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaterConnectionValidatorTest {

    @InjectMocks
    private WaterConnectionValidator validator;

    @Mock private PropertyValidator propertyValidator;
    @Mock private WaterFieldValidator waterFieldValidator;
    @Mock private MeterInfoValidator meterInfoValidator;
    @Mock private MultiStateInstanceUtil centralInstanceUtil;
    @Mock private EncryptionDecryptionUtil encryptionDecryptionUtil;

    @Test
    void validateSearch_whenCentralInstanceAndTenantMissing_throwsCustomException() {
        when(centralInstanceUtil.getIsEnvironmentCentralInstance()).thenReturn(true);
        SearchCriteria criteria = SearchCriteria.builder().build();
        assertThatThrownBy(() -> validator.validateSearch(criteria))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void validateSearch_whenCentralInstanceAndTenantTooShort_throwsInvalidSearch() {
        when(centralInstanceUtil.getIsEnvironmentCentralInstance()).thenReturn(true);
        when(centralInstanceUtil.getStateLevelTenantIdLength()).thenReturn(2);
        SearchCriteria criteria = SearchCriteria.builder().tenantId("pb").build();

        assertThatThrownBy(() -> validator.validateSearch(criteria))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("TenantId should be mandatorily");
    }

    @Test
    void validateWaterConnection_whenProcessInstanceMissing_throwsInvalidAction() {
        WaterConnection waterConnection = WaterConnection.builder().build();
        WaterConnectionRequest request = WaterConnectionRequest.builder()
                .requestInfo(RequestInfo.builder().userInfo(User.builder().build()).build())
                .waterConnection(waterConnection)
                .build();

        assertThatThrownBy(() -> validator.validateWaterConnection(request, WCConstants.CREATE_APPLICATION))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void validateWaterConnection_whenPayActionDirectly_throwsInvalidAction() {
        WaterConnection waterConnection = new WaterConnection();
        waterConnection.setProcessInstance(ProcessInstance.builder().action("PAY").build());
        waterConnection.setPropertyId("prop-1");
        WaterConnectionRequest request = WaterConnectionRequest.builder()
                .requestInfo(RequestInfo.builder().userInfo(User.builder().type("EMPLOYEE").build()).build())
                .waterConnection(waterConnection)
                .build();
        when(propertyValidator.validate(any(), anyInt())).thenReturn(new ValidatorResult(true, null));
        when(waterFieldValidator.validate(any(), anyInt())).thenReturn(new ValidatorResult(true, null));
        when(meterInfoValidator.validate(any(), anyInt())).thenReturn(new ValidatorResult(true, null));

        assertThatThrownBy(() -> validator.validateWaterConnection(request, WCConstants.CREATE_APPLICATION))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void validateUpdate_whenDuplicateDocuments_throwsDuplicateDocumentError() {
        WaterConnection searchResult = new WaterConnection();
        searchResult.setApplicationNo("WS-1");
        WaterConnection update = new WaterConnection();
        update.setApplicationNo("WS-1");
        update.setDocuments(List.of(
                Document.builder().fileStoreId("fs-1").build(),
                Document.builder().fileStoreId("fs-1").build()));
        WaterConnectionRequest request = WaterConnectionRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .waterConnection(update)
                .build();

        assertThatThrownBy(() -> validator.validateUpdate(request, searchResult, WCConstants.UPDATE_APPLICATION))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Same document cannot be used multiple times");
    }

    @Test
    void validateUpdate_whenApplicationNumberMismatch_throwsInvalidUpdate() {
        WaterConnection searchResult = new WaterConnection();
        searchResult.setApplicationNo("WS-1");
        WaterConnection update = new WaterConnection();
        update.setApplicationNo("WS-2");
        update.setDocuments(List.of());
        WaterConnectionRequest request = WaterConnectionRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .waterConnection(update)
                .build();

        assertThatThrownBy(() -> validator.validateUpdate(request, searchResult, WCConstants.UPDATE_APPLICATION))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void validatePropertyForConnection_whenPropertyIdMissing_logsWithoutThrowing() {
        WaterConnection connection = new WaterConnection();
        connection.setApplicationNo("WS-LOG");

        assertThatCode(() -> validator.validatePropertyForConnection(List.of(connection)))
                .doesNotThrowAnyException();
    }
}
