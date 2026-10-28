package org.egov.inbox.repository.builder.V2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.inbox.util.MDMSUtil;
import org.egov.inbox.web.model.InboxRequest;
import org.egov.inbox.web.model.InboxSearchCriteria;
import org.egov.inbox.web.model.V2.InboxQueryConfiguration;
import org.egov.inbox.web.model.V2.SearchParam;
import org.egov.inbox.web.model.V2.SortParam;
import org.egov.inbox.web.model.workflow.ProcessInstanceSearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InboxQueryBuilderTest {

    @Mock
    private MDMSUtil mdmsUtil;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private InboxQueryBuilder inboxQueryBuilder;

    private InboxRequest inboxRequest;

    @BeforeEach
    void setUpRequest() {
        when(mdmsUtil.getConfigFromMDMS(anyString(), anyString())).thenReturn(InboxQueryConfiguration.builder()
                .allowedSearchCriteria(List.of(
                        SearchParam.builder().name("tenantId").path("Data.tenantId").operator(SearchParam.Operator.EQUAL).build()))
                .sortParam(SortParam.builder().path("Data.auditDetails.createdTime").order(SortParam.Order.DESC).build())
                .sourceFilterPathList(List.of("Data.applicationNumber"))
                .build());

        InboxSearchCriteria inbox = new InboxSearchCriteria();
        inbox.setTenantId("pb.amritsar");
        inbox.setOffset(0);
        inbox.setLimit(10);
        ProcessInstanceSearchCriteria processCriteria = new ProcessInstanceSearchCriteria();
        processCriteria.setModuleName("pt-services");
        processCriteria.setTenantId("pb.amritsar");
        inbox.setProcessSearchCriteria(processCriteria);
        inbox.setModuleSearchCriteria(new HashMap<>());
        inboxRequest = InboxRequest.builder().inbox(inbox).build();
    }

    @Test
    void getStatusCountQuery_includesAggregationNode() {
        Map<String, Object> query = inboxQueryBuilder.getStatusCountQuery(inboxRequest);

        assertThat(query).containsKey("aggs");
        assertThat(query).containsKey("query");
    }

    @Test
    void getESQuery_withoutPagination_buildsBoolQuery() {
        Map<String, Object> query = inboxQueryBuilder.getESQuery(inboxRequest, false);

        assertThat(query).containsKey("query");
        assertThat(query).doesNotContainKeys("from", "size");
    }
}
