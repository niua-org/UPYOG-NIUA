package org.egov.pgr.util;

import org.egov.common.utils.MultiStateInstanceUtil;
import org.egov.pgr.web.models.AuditDetails;
import org.egov.pgr.web.models.Service;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PGRUtilsTest {

    @Mock
    private MultiStateInstanceUtil multiStateInstanceUtil;

    @InjectMocks
    private PGRUtils pgrUtils;

    @Test
    void getAuditDetails_whenCreate_setsCreatedAndModifiedTimes() {
        AuditDetails audit = pgrUtils.getAuditDetails("user-1", Service.builder().build(), true);

        assertThat(audit.getCreatedBy()).isEqualTo("user-1");
        assertThat(audit.getLastModifiedBy()).isEqualTo("user-1");
        assertThat(audit.getCreatedTime()).isNotNull();
        assertThat(audit.getLastModifiedTime()).isEqualTo(audit.getCreatedTime());
    }

    @Test
    void getAuditDetails_whenUpdate_preservesCreatedFields() {
        AuditDetails existing = AuditDetails.builder()
                .createdBy("creator")
                .createdTime(100L)
                .build();
        Service service = Service.builder().auditDetails(existing).build();

        AuditDetails audit = pgrUtils.getAuditDetails("updater", service, false);

        assertThat(audit.getCreatedBy()).isEqualTo("creator");
        assertThat(audit.getCreatedTime()).isEqualTo(100L);
        assertThat(audit.getLastModifiedBy()).isEqualTo("updater");
        assertThat(audit.getLastModifiedTime()).isGreaterThan(100L);
    }

    @Test
    void replaceSchemaPlaceholder_whenUtilSucceeds_returnsQuery() throws Exception {
        when(multiStateInstanceUtil.replaceSchemaPlaceholder("select * from {schema}.t", "pg.citya"))
                .thenReturn("select * from pg_citya.t");

        assertThat(pgrUtils.replaceSchemaPlaceholder("select * from {schema}.t", "pg.citya"))
                .isEqualTo("select * from pg_citya.t");
    }

    @Test
    void replaceSchemaPlaceholder_whenUtilFails_throwsInvalidTenant() throws Exception {
        when(multiStateInstanceUtil.replaceSchemaPlaceholder("q", "bad"))
                .thenThrow(new RuntimeException("bad tenant"));

        assertThatThrownBy(() -> pgrUtils.replaceSchemaPlaceholder("q", "bad"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Invalid tenantId");
    }
}
