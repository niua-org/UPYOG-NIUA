package org.egov.pgr.utils;

import org.egov.pgr.model.AuditDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PGRUtilsTest {

    @InjectMocks
    private PGRUtils pgrUtils;

    @Test
    void getAuditDetails_whenCreate_setsCreatedFields() {
        AuditDetails audit = pgrUtils.getAuditDetails("emp-1", true);

        assertThat(audit.getCreatedBy()).isEqualTo("emp-1");
        assertThat(audit.getLastModifiedBy()).isEqualTo("emp-1");
    }

    @Test
    void splitCamelCase_insertsSpaces() {
        assertThat(PGRUtils.splitCamelCase("ServiceRequestId")).isEqualTo("Service Request Id");
    }

    @Test
    void convertToMilliSec_convertsHours() {
        assertThat(pgrUtils.convertToMilliSec(2)).isEqualTo(TimeUnit.HOURS.toMillis(2));
    }

    @Test
    void getReceptorsOfNotification_forResolvedStatus_includesCitizen() {
        Set<String> receptors = pgrUtils.getReceptorsOfNotification("resolved", "resolve");

        assertThat(receptors).contains("CITIZEN");
    }
}
