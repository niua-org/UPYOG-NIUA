package org.egov.garbageservice.util;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.garbageservice.web.models.AuditDetails;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GrbgUtilsTest {

    @Test
    void getAuditDetails_whenCreate_populatesCreatedAndModified() {
        AuditDetails audit = GrbgUtils.getAuditDetails("user-1", true);

        assertThat(audit.getCreatedBy()).isEqualTo("user-1");
        assertThat(audit.getLastModifiedBy()).isEqualTo("user-1");
        assertThat(audit.getCreatedTime()).isNotNull();
    }

    @Test
    void buildCreateAuditDetails_usesRequestInfoUserUuid() {
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(User.builder().uuid("uuid-1").build())
                .build();

        AuditDetails audit = new GrbgUtils().buildCreateAuditDetails(requestInfo);

        assertThat(audit.getCreatedBy()).isEqualTo("uuid-1");
        assertThat(audit.getLastModifiedBy()).isEqualTo("uuid-1");
    }
}
