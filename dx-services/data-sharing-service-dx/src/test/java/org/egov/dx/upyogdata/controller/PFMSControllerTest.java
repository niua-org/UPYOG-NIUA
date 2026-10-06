package org.egov.dx.upyogdata.controller;

import org.egov.dx.upyogdata.pfms.models.PFMSCreateTransactionRequest;
import org.egov.dx.upyogdata.pfms.service.PFMSForwardingService;
import org.egov.dx.upyogdata.pfms.service.PFMSService;
import org.egov.externalaudit.model.ExternalIntegrationContext;
import org.egov.externalaudit.service.ExternalApiAuditLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PFMSControllerTest {

    @Mock
    private PFMSService pfmsService;

    @Mock
    private PFMSForwardingService pfmsForwardingService;

    @Mock
    private ExternalApiAuditLogger auditLogger;

    @InjectMocks
    private PFMSController pfmsController;

    @Test
    void createTransaction_shouldAuditInboundStateCall() {
        PFMSCreateTransactionRequest request = new PFMSCreateTransactionRequest();
        when(auditLogger.logInboundApi(any(), any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(1);
            return supplier.get();
        });
        when(pfmsService.createTransaction(request)).thenReturn("SUCCESS");

        ResponseEntity<?> response = pfmsController.createTransaction(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("SUCCESS", response.getBody());
        ArgumentCaptor<ExternalIntegrationContext> contextCaptor = ArgumentCaptor.forClass(ExternalIntegrationContext.class);
        verify(auditLogger).logInboundApi(contextCaptor.capture(), any());
        assertEquals("PFMS", contextCaptor.getValue().getTenantId());
        assertEquals("state-pfms-transaction-create", contextCaptor.getValue().getExternalApiName());
        assertEquals("POST", contextCaptor.getValue().getHttpMethod());
    }
}
