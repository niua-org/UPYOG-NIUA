package org.bel.birthdeath.utils;

import org.bel.birthdeath.common.model.AuditDetails;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommonUtilsTest {

    private final CommonUtils utils = new CommonUtils();

    @Test
    void addfullName_concatenatesNonNullParts() {
        assertThat(utils.addfullName("A", "B", "C")).isEqualTo("A B C");
    }

    @Test
    void getAuditDetails_whenCreate_setsCreatedBy() {
        AuditDetails audit = utils.getAuditDetails("user", true);
        assertThat(audit.getCreatedBy()).isEqualTo("user");
    }
}
