package org.egov.ewst.util;

import org.egov.ewst.config.EwasteConfiguration;
import org.egov.ewst.models.AuditDetails;
import org.egov.ewst.repository.ServiceRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EwasteUtilTest {

    @Mock
    private EwasteConfiguration configs;
    @Mock
    private ServiceRequestRepository restRepo;

    @Test
    void getAuditDetails_whenCreate_setsCreatedBy() {
        EwasteUtil util = new EwasteUtil(new ObjectMapper(), configs, restRepo);
        AuditDetails audit = util.getAuditDetails("user", true);
        assertThat(audit.getCreatedBy()).isEqualTo("user");
    }
}
