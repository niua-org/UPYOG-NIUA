package org.egov.fsm.util;

import org.egov.common.contract.request.RequestInfo;
import org.egov.fsm.config.FSMConfiguration;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FSMUtilTest {

    @Mock
    private FSMConfiguration config;

    @InjectMocks
    private FSMUtil fsmUtil;

    @Test
    void getAuditDetails_whenCreate_setsCreatedAndModifiedFields() {
        var audit = fsmUtil.getAuditDetails("user-uuid", true);

        assertThat(audit.getCreatedBy()).isEqualTo("user-uuid");
        assertThat(audit.getLastModifiedBy()).isEqualTo("user-uuid");
        assertThat(audit.getCreatedTime()).isNotNull();
        assertThat(audit.getLastModifiedTime()).isEqualTo(audit.getCreatedTime());
    }

    @Test
    void getAuditDetails_whenUpdate_setsOnlyModifiedFields() {
        var audit = fsmUtil.getAuditDetails("editor-uuid", false);

        assertThat(audit.getCreatedBy()).isNull();
        assertThat(audit.getLastModifiedBy()).isEqualTo("editor-uuid");
        assertThat(audit.getLastModifiedTime()).isNotNull();
    }

    @Test
    void getMdmsSearchUrl_concatenatesHostAndEndpoint() {
        when(config.getMdmsHost()).thenReturn("http://mdms/");
        when(config.getMdmsEndPoint()).thenReturn("egov-mdms-service/v1/_search");

        assertThat(fsmUtil.getMdmsSearchUrl().toString())
                .isEqualTo("http://mdms/egov-mdms-service/v1/_search");
    }

    @Test
    void getMDMSRequest_buildsFsmAndVehicleModuleCriteria() {
        MdmsCriteriaReq req = fsmUtil.getMDMSRequest(RequestInfo.builder().build(), "pb.amritsar");

        assertThat(req.getMdmsCriteria().getTenantId()).isEqualTo("pb.amritsar");
        assertThat(req.getMdmsCriteria().getModuleDetails()).hasSize(2);
    }

    @Test
    void getFSMModuleRequest_includesFsmAndVehicleMasters() {
        assertThat(fsmUtil.getFSMModuleRequest()).hasSize(2);
    }

    @Test
    void defaultJsonPathConfig_doesNotThrow() {
        fsmUtil.defaultJsonPathConfig();
    }
}
