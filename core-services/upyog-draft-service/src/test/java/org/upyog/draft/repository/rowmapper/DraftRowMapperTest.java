package org.upyog.draft.repository.rowmapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.draft.web.models.DraftDetail;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DraftRowMapperTest {

    @Mock
    private ResultSet resultSet;

    private DraftRowMapper rowMapper;

    @BeforeEach
    void setUp() {
        rowMapper = new DraftRowMapper(new ObjectMapper());
    }

    @Test
    void mapRow_validJsonDraftData_deserializesToMap() throws Exception {
        stubColumns("{\"step\":1,\"module\":\"PT\"}");
        when(resultSet.getBigDecimal("completion_pct")).thenReturn(new BigDecimal("42.5"));
        when(resultSet.getLong("createdtime")).thenReturn(100L);
        when(resultSet.getLong("lastmodifiedtime")).thenReturn(200L);

        DraftDetail draft = rowMapper.mapRow(resultSet, 1);

        assertThat(draft.getDraftId()).isEqualTo("draft-1");
        assertThat(draft.getTenantId()).isEqualTo("pb.amritsar");
        assertThat(draft.getDraftData()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> draftData = (Map<String, Object>) draft.getDraftData();
        assertThat(draftData).containsEntry("step", 1);
        assertThat(draft.getCompletionPct()).isEqualByComparingTo("42.5");
        assertThat(draft.getCreatedTime()).isEqualTo(100L);
        assertThat(draft.getLastModifiedTime()).isEqualTo(200L);
    }

    @Test
    void mapRow_nullDraftData_leavesDraftDataNull() throws Exception {
        stubColumns(null);
        when(resultSet.getBigDecimal("completion_pct")).thenReturn(null);
        when(resultSet.getLong("createdtime")).thenReturn(0L);
        when(resultSet.getLong("lastmodifiedtime")).thenReturn(0L);

        DraftDetail draft = rowMapper.mapRow(resultSet, 1);

        assertThat(draft.getDraftData()).isNull();
    }

    @Test
    void mapRow_invalidJson_fallsBackToRawString() throws Exception {
        stubColumns("{not-json");
        when(resultSet.getBigDecimal("completion_pct")).thenReturn(BigDecimal.ZERO);
        when(resultSet.getLong("createdtime")).thenReturn(1L);
        when(resultSet.getLong("lastmodifiedtime")).thenReturn(2L);

        DraftDetail draft = rowMapper.mapRow(resultSet, 1);

        assertThat(draft.getDraftData()).isEqualTo("{not-json");
    }

    private void stubColumns(String draftDataJson) throws Exception {
        when(resultSet.getString("draft_id")).thenReturn("draft-1");
        when(resultSet.getString("tenant_id")).thenReturn("pb.amritsar");
        when(resultSet.getString("business_service")).thenReturn("PT");
        when(resultSet.getString("module_name")).thenReturn("property");
        when(resultSet.getString("module_entity_id")).thenReturn("entity-1");
        when(resultSet.getString("creator_type")).thenReturn("USER");
        when(resultSet.getString("draft_data")).thenReturn(draftDataJson);
        when(resultSet.getString("status")).thenReturn("ACTIVE");
        when(resultSet.getString("createdby")).thenReturn("owner");
        when(resultSet.getString("lastmodifiedby")).thenReturn("owner");
    }
}
