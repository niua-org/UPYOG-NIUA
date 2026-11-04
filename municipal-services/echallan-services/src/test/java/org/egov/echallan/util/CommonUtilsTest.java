package org.egov.echallan.util;

import org.egov.echallan.model.AuditDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CommonUtilsTest {

    @InjectMocks
    private CommonUtils commonUtils;

    @Test
    void getAuditDetails_whenCreate_populatesFields() {
        AuditDetails audit = commonUtils.getAuditDetails("user", true);
        assertThat(audit.getCreatedBy()).isEqualTo("user");
    }
}
