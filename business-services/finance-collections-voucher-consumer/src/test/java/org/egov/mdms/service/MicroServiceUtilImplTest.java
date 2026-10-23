package org.egov.mdms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.receipt.consumer.model.FinanceMdmsModel;
import org.egov.receipt.consumer.model.RequestInfo;
import org.egov.receipt.consumer.support.ReceiptTestFixtures;
import org.egov.receipt.custom.exception.VoucherCustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MicroServiceUtilImplTest {

    @Mock
    private org.egov.reciept.consumer.config.PropertiesManager manager;
    @Mock
    private org.egov.receipt.consumer.model.MdmsCriteria mdmscriteria;
    @Mock
    private org.egov.receipt.consumer.model.MdmsCriteriaReq mdmsrequest;
    @Mock
    private org.egov.receipt.consumer.repository.ServiceRequestRepository serviceRequestRepository;

    @InjectMocks
    private MicroServiceUtilImpl microServiceUtil;

    @BeforeEach
    void wireMapper() {
        org.springframework.test.util.ReflectionTestUtils.setField(microServiceUtil, "mapper", new ObjectMapper());
    }

    @Test
    void getBusinessService_readsPreloadedMdmsPayload() throws VoucherCustomException {
        FinanceMdmsModel mdms = new FinanceMdmsModel();
        mdms.setFinanceServiceMdmsData(ReceiptTestFixtures.mdmsWithBusinessMapping("PT", true));

        var services = microServiceUtil.getBusinessService("pb.amritsar", "PT", new RequestInfo(), mdms);

        assertThat(services).hasSize(1);
        assertThat(services.get(0).getCode()).isEqualTo("PT");
    }

    @Test
    void getBusinessServiceName_readsBillingServiceMaster() throws VoucherCustomException {
        FinanceMdmsModel mdms = new FinanceMdmsModel();
        mdms.setFinanceServiceMdmsData(ReceiptTestFixtures.mdmsWithBusinessMapping("PT", true));

        String name = microServiceUtil.getBusinessServiceName("pb.amritsar", "PT", new RequestInfo(), mdms);

        assertThat(name).isEqualTo("Property Tax");
    }
}
