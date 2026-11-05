package org.egov.vendor.utils;

import org.egov.vendor.web.models.AuditDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class VendorUtilTest {

    @InjectMocks
    private VendorUtil vendorUtil;

    @Test
    void getAuditDetails_whenCreate_setsCreatedBy() {
        AuditDetails audit = vendorUtil.getAuditDetails("user", true);
        assertThat(audit.getCreatedBy()).isEqualTo("user");
    }
}
