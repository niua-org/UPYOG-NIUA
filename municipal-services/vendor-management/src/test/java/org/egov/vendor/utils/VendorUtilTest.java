package org.egov.vendor.utils;

import org.egov.vendor.web.models.AuditDetails;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("getAuditDetails_whenCreate_setsCreatedAndModifiedFields")
    void getAuditDetails_whenCreate_setsCreatedAndModifiedFields() {
        AuditDetails audit = vendorUtil.getAuditDetails("user-1", true);

        assertThat(audit.getCreatedBy()).isEqualTo("user-1");
        assertThat(audit.getLastModifiedBy()).isEqualTo("user-1");
        assertThat(audit.getCreatedTime()).isNotNull();
        assertThat(audit.getLastModifiedTime()).isEqualTo(audit.getCreatedTime());
    }

    @Test
    @DisplayName("getAuditDetails_whenUpdate_omitsCreatedFields")
    void getAuditDetails_whenUpdate_omitsCreatedFields() {
        AuditDetails audit = vendorUtil.getAuditDetails("user-2", false);

        assertThat(audit.getCreatedBy()).isNull();
        assertThat(audit.getLastModifiedBy()).isEqualTo("user-2");
        assertThat(audit.getLastModifiedTime()).isNotNull();
    }
}
