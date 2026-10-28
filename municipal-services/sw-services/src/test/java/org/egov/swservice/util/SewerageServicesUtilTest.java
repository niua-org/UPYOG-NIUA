package org.egov.swservice.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.swservice.config.SWConfiguration;
import org.egov.swservice.repository.ServiceRequestRepository;
import org.egov.swservice.web.models.AuditDetails;
import org.egov.swservice.web.models.PropertyCriteria;
import org.egov.swservice.web.models.SearchCriteria;
import org.egov.swservice.web.models.SewerageConnection;
import org.egov.swservice.web.models.SewerageConnectionRequest;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class SewerageServicesUtilTest {

    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private SWConfiguration config;

    private SewerageServicesUtil util;

    @BeforeEach
    void setUp() {
        util = new SewerageServicesUtil(serviceRequestRepository);
        ReflectionTestUtils.setField(util, "mapper", new ObjectMapper());
        ReflectionTestUtils.setField(util, "propertyHost", "http://property/");
        ReflectionTestUtils.setField(util, "searchPropertyEndPoint", "search");
        ReflectionTestUtils.setField(util, "createPropertyEndPoint", "create");
        ReflectionTestUtils.setField(util, "config", config);
    }

    @Test
    void propertySearchOnCriteria_whenNoSearchFields_returnsEmpty() {
        SearchCriteria criteria = SearchCriteria.builder().tenantId("pb.amritsar").build();

        assertThat(util.propertySearchOnCriteria(criteria, RequestInfo.builder().build())).isEmpty();
    }

    @Test
    void getPropertyDetails_whenInvalidJson_throwsParsingError() {
        assertThatThrownBy(() -> util.getPropertyDetails(Collections.singletonMap("bad", "shape")))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("PARSING_ERROR");
    }

    @Test
    void getAuditDetails_whenCreate_populatesCreatedFields() {
        AuditDetails audit = util.getAuditDetails("user-1", true);

        assertThat(audit.getCreatedBy()).isEqualTo("user-1");
        assertThat(audit.getLastModifiedBy()).isEqualTo("user-1");
        assertThat(audit.getCreatedTime()).isNotNull();
    }

    @Test
    void getAuditDetails_whenUpdate_omitsCreatedBy() {
        AuditDetails audit = util.getAuditDetails("user-2", false);

        assertThat(audit.getCreatedBy()).isNull();
        assertThat(audit.getLastModifiedBy()).isEqualTo("user-2");
    }

    @Test
    void getPropertyURL_buildsQueryStringFromCriteria() {
        PropertyCriteria criteria = PropertyCriteria.builder()
                .tenantId("pb.amritsar")
                .propertyIds(new HashSet<>(Set.of("prop-1")))
                .mobileNumber("9999999999")
                .build();

        String url = util.getPropertyURL(criteria).toString();

        assertThat(url).startsWith("http://property/search?");
        assertThat(url).contains("tenantId=pb.amritsar");
        assertThat(url).contains("propertyIds=prop-1");
        assertThat(url).contains("mobileNumber=9999999999");
    }

    @Test
    void isModifyConnectionRequest_whenConnectionNoPresent_returnsTrue() {
        SewerageConnectionRequest request = SewerageConnectionRequest.builder()
                .sewerageConnection(SewerageConnection.builder().connectionNo("SW-001").build())
                .build();

        assertThat(util.isModifyConnectionRequest(request)).isTrue();
    }

    @Test
    void prepareMdMsRequest_buildsModuleDetails() {
        var req = util.prepareMdMsRequest("pb", "ws-services-masters", List.of("connectionType"), "$.*.code",
                RequestInfo.builder().build());

        assertThat(req.getMdmsCriteria().getTenantId()).isEqualTo("pb");
        assertThat(req.getMdmsCriteria().getModuleDetails()).hasSize(1);
        assertThat(req.getMdmsCriteria().getModuleDetails().get(0).getMasterDetails()).hasSize(1);
    }
}
