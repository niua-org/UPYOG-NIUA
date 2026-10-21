package org.egov.receipt.consumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.mdms.service.MicroServiceUtil;
import org.egov.receipt.consumer.model.FinanceMdmsModel;
import org.egov.receipt.consumer.model.ProcessStatus;
import org.egov.receipt.consumer.model.Tenant;
import org.egov.receipt.consumer.support.ReceiptTestFixtures;
import org.egov.receipt.custom.exception.VoucherCustomException;
import org.egov.reciept.consumer.config.PropertiesManager;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherServiceImplTest {

    @Mock
    private PropertiesManager propertiesManager;
    @Mock
    private MicroServiceUtil microServiceUtil;
    @Mock
    private org.egov.receipt.consumer.repository.ServiceRequestRepository serviceRequestRepository;

    @InjectMocks
    private VoucherServiceImpl voucherService;

    @BeforeEach
    void wireMapper() {
        org.springframework.test.util.ReflectionTestUtils.setField(voucherService, "mapper", new ObjectMapper());
    }

    @Test
    void isVoucherCreationEnabled_trueWhenMappingAllows() throws Exception {
        var req = ReceiptTestFixtures.receiptReq("pb.amritsar", "PT");
        FinanceMdmsModel mdms = new FinanceMdmsModel();
        mdms.setFinanceServiceMdmsData(ReceiptTestFixtures.mdmsWithBusinessMapping("PT", true));

        when(microServiceUtil.getBusinessService(eq("pb.amritsar"), eq("PT"), any(), eq(mdms)))
                .thenReturn(List.of(ReceiptTestFixtures.businessService("PT", true)));

        assertThat(voucherService.isVoucherCreationEnabled(req.getReceipt().get(0), req.getRequestInfo(), mdms))
                .isTrue();
    }

    @Test
    void isVoucherCreationEnabled_falseWhenMappingDisabled() throws Exception {
        var req = ReceiptTestFixtures.receiptReq("pb.amritsar", "PT");
        FinanceMdmsModel mdms = new FinanceMdmsModel();
        when(microServiceUtil.getBusinessService(eq("pb.amritsar"), eq("PT"), any(), eq(mdms)))
                .thenReturn(List.of(ReceiptTestFixtures.businessService("PT", false)));

        assertThat(voucherService.isVoucherCreationEnabled(req.getReceipt().get(0), req.getRequestInfo(), mdms))
                .isFalse();
    }

    @Test
    void isTenantEnabledInFinanceModule_throwsWhenTenantMissing() throws Exception {
        var req = ReceiptTestFixtures.receiptReq("pb.amritsar", "PT");
        FinanceMdmsModel mdms = new FinanceMdmsModel();
        when(microServiceUtil.getFinanceTenantList(eq("pb.amritsar"), eq("PT"), any(), eq(mdms)))
                .thenReturn(List.of(ReceiptTestFixtures.tenant("pb.other")));

        assertThatThrownBy(() -> voucherService.isTenantEnabledInFinanceModule(req, mdms))
                .isInstanceOf(VoucherCustomException.class)
                .hasMessageContaining("not enabled");
    }

    @Test
    void isTenantEnabledInFinanceModule_trueWhenTenantPresent() throws Exception {
        var req = ReceiptTestFixtures.receiptReq("pb.amritsar", "PT");
        FinanceMdmsModel mdms = new FinanceMdmsModel();
        when(microServiceUtil.getFinanceTenantList(eq("pb.amritsar"), eq("PT"), any(), eq(mdms)))
                .thenReturn(List.of(ReceiptTestFixtures.tenant("pb.amritsar")));

        assertThat(voucherService.isTenantEnabledInFinanceModule(req, mdms)).isTrue();
    }
}
