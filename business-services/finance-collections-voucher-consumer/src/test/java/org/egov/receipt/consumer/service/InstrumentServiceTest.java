package org.egov.receipt.consumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.mdms.service.MicroServiceUtil;
import org.egov.receipt.consumer.model.FinanceMdmsModel;
import org.egov.receipt.consumer.model.FinancialStatus;
import org.egov.receipt.consumer.model.InstrumentResponse;
import org.egov.receipt.consumer.repository.ServiceRequestRepository;
import org.egov.receipt.consumer.support.ReceiptTestFixtures;
import org.egov.reciept.consumer.config.PropertiesManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstrumentServiceTest {

    @Mock
    private PropertiesManager propertiesManager;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Spy
    private ObjectMapper mapper = new ObjectMapper();
    @Mock
    private MicroServiceUtil microServiceUtil;
    @Mock
    private VoucherService voucherService;

    @InjectMocks
    private InstrumentService instrumentService;

    @BeforeEach
    void stubHosts() {
        when(propertiesManager.getInstrumentHostUrl()).thenReturn("http://instrument/");
        when(propertiesManager.getInstrumentCreate()).thenReturn("instrument/_create");
    }

    @Test
    void createInstrument_validReceipt_buildsRequestAndReturnsResponse() throws Exception {
        var receiptReq = ReceiptTestFixtures.receiptReqWithInstrument("pb.amritsar", "PT");
        FinancialStatus status = new FinancialStatus();
        status.setCode("New");
        when(microServiceUtil.getFinancialStatusByCode(anyString(), any(), any(), anyString())).thenReturn(status);

        InstrumentResponse apiResponse = new InstrumentResponse();
        apiResponse.setInstruments(Collections.emptyList());
        when(serviceRequestRepository.fetchResult(any(), any(), anyString())).thenReturn(apiResponse);

        InstrumentResponse response = instrumentService.createInstrument(
                receiptReq, null, new FinanceMdmsModel(), "V1");

        assertThat(response).isNotNull();
    }
}
