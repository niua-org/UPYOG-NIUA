package org.egov.pt.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.egov.common.contract.request.RequestInfo;
import org.egov.pt.config.PropertyConfiguration;
import org.egov.pt.models.OwnerInfo;
import org.egov.pt.models.Property;
import org.egov.pt.repository.ServiceRequestRepository;
import org.egov.pt.web.contracts.IdGenerationResponse;
import org.egov.pt.web.contracts.IdResponse;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommonUtilsTest {

    @Mock
    private ObjectMapper mapper;
    @Mock
    private PropertyConfiguration configs;
    @Mock
    private ServiceRequestRepository restRepo;

    @InjectMocks
    private CommonUtils commonUtils;

    @BeforeEach
    void stubIdGenUrls() {
        when(configs.getIdGenHost()).thenReturn("http://idgen/");
        when(configs.getIdGenPath()).thenReturn("id");
        when(configs.getMdmsHost()).thenReturn("http://mdms/");
        when(configs.getMdmsEndPoint()).thenReturn("v1");
    }

    @Test
    void getAuditDetails_whenCreate_setsCreatedAndModifiedFields() {
        var audit = commonUtils.getAuditDetails("user-1", true);
        assertThat(audit.getCreatedBy()).isEqualTo("user-1");
        assertThat(audit.getLastModifiedBy()).isEqualTo("user-1");
        assertThat(audit.getCreatedTime()).isNotNull();
        assertThat(audit.getLastModifiedTime()).isEqualTo(audit.getCreatedTime());
    }

    @Test
    void getAuditDetails_whenUpdate_setsOnlyModifiedFields() {
        var audit = commonUtils.getAuditDetails("user-2", false);
        assertThat(audit.getCreatedBy()).isNull();
        assertThat(audit.getLastModifiedBy()).isEqualTo("user-2");
        assertThat(audit.getLastModifiedTime()).isNotNull();
    }

    @Test
    void getUserForWorkflow_whenOwnersPresent_includesAccountIdUuid() {
        Property property = Property.builder()
                .accountId("acc-uuid")
                .owners(List.of(OwnerInfo.builder().uuid("o1").build()))
                .build();

        List<OwnerInfo> users = commonUtils.getUserForWorkflow(property);

        assertThat(users).hasSize(2);
        assertThat(users.get(1).getUuid()).isEqualTo("acc-uuid");
    }

    @Test
    void jsonMerge_whenMainNull_returnsUpdateNode() {
        ObjectNode update = new ObjectMapper().createObjectNode().put("k", "v");
        assertThat(commonUtils.jsonMerge(null, update)).isEqualTo(update);
    }

    @Test
    void jsonMerge_whenUpdateOverwritesField_changesMainNode() {
        ObjectMapper om = new ObjectMapper();
        ObjectNode main = om.createObjectNode().put("a", "old");
        ObjectNode update = om.createObjectNode().put("a", "new");
        commonUtils.jsonMerge(main, update);
        assertThat(main.get("a").asText()).isEqualTo("new");
    }

    @Test
    void getIdList_whenIdGenReturnsEmpty_throwsCustomException() {
        when(restRepo.fetchResult(any(), any())).thenReturn(Optional.of(Map.of()));
        when(mapper.convertValue(any(), org.mockito.ArgumentMatchers.eq(IdGenerationResponse.class)))
                .thenReturn(IdGenerationResponse.builder().idResponses(Collections.emptyList()).build());

        assertThatThrownBy(
                        () -> commonUtils.getIdList(RequestInfo.builder().build(), "pb.amritsar", "pid", "PT-[cy:yyyy]", 1))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("No ids returned");
    }

    @Test
    void getIdList_whenIdGenReturnsIds_returnsIdStrings() {
        when(restRepo.fetchResult(any(), any())).thenReturn(Optional.of(Map.of()));
        when(mapper.convertValue(any(), org.mockito.ArgumentMatchers.eq(IdGenerationResponse.class)))
                .thenReturn(
                        IdGenerationResponse.builder()
                                .idResponses(List.of(IdResponse.builder().id("PT-001").build()))
                                .build());

        List<String> ids = commonUtils.getIdList(RequestInfo.builder().build(), "pb.amritsar", "pid", "fmt", 1);

        assertThat(ids).containsExactly("PT-001");
    }
}
