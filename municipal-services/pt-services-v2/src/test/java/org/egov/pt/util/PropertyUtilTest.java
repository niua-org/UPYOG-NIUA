package org.egov.pt.util;

import org.egov.common.contract.request.RequestInfo;
import org.egov.pt.web.models.AuditDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PropertyUtilTest {

    @InjectMocks
    private PropertyUtil propertyUtil;

    @Test
    void getAuditDetails_whenCreate_populatesFields() {
        AuditDetails audit = propertyUtil.getAuditDetails("user", true);
        assertThat(audit.getCreatedBy()).isEqualTo("user");
    }

    @Test
    void prepareMdMsRequest_buildsModuleCriteria() {
        RequestInfo ri = RequestInfo.builder().apiId("pt").build();
        var req = propertyUtil.prepareMdMsRequest("pb", "PropertyTax", List.of("UsageCategory"), null, ri);
        assertThat(req.getMdmsCriteria().getTenantId()).isEqualTo("pb");
    }
}
