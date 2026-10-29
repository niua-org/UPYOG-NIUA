package org.egov.asset.calculator.utils;

import org.egov.asset.calculator.web.models.AuditDetails;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalculationUtilsTest {

    private final CalculationUtils utils = new CalculationUtils();

    @Test
    void getAuditDetails_whenCreate_setsCreatedFields() {
        AuditDetails audit = utils.getAuditDetails("user", true);
        assertThat(audit.getCreatedBy()).isEqualTo("user");
        assertThat(audit.getCreatedTime()).isNotNull();
    }
}
