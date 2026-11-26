package org.egov.pqm.anomaly.finder.util;

import org.egov.pqm.anomaly.finder.web.model.AuditDetails;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PqmAnomalyFinderUtilTest {

    private final PqmAnomalyFinderUtil util = new PqmAnomalyFinderUtil();

    @Test
    void getAuditDetails_whenCreate_setsCreatedBy() {
        AuditDetails audit = util.getAuditDetails("user", true);
        assertThat(audit.getCreatedBy()).isEqualTo("user");
    }

    @Test
    void defaultJsonPathConfig_doesNotThrow() {
        util.defaultJsonPathConfig();
    }
}
