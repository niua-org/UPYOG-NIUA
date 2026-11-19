package org.egov.infra.mdms.repository.impl;

import net.minidev.json.JSONArray;
import org.egov.infra.mdms.config.ApplicationConfig;
import org.egov.infra.mdms.model.Mdms;
import org.egov.infra.mdms.model.MdmsCriteria;
import org.egov.infra.mdms.model.MdmsCriteriaV2;
import org.egov.infra.mdms.model.MdmsRequest;
import org.egov.infra.mdms.producer.Producer;
import org.egov.infra.mdms.repository.querybuilder.MdmsDataQueryBuilder;
import org.egov.infra.mdms.repository.querybuilder.MdmsDataQueryBuilderV2;
import org.egov.infra.mdms.repository.rowmapper.MdmsDataRowMapper;
import org.egov.infra.mdms.repository.rowmapper.MdmsDataRowMapperV2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MdmsDataRepositoryImplTest {

    @Mock
    private Producer producer;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private ApplicationConfig applicationConfig;
    @Mock
    private MdmsDataQueryBuilder mdmsDataQueryBuilder;
    @Mock
    private MdmsDataQueryBuilderV2 mdmsDataQueryBuilderV2;
    @Mock
    private MdmsDataRowMapperV2 mdmsDataRowMapperV2;
    @Mock
    private MdmsDataRowMapper mdmsDataRowMapper;

    private MdmsDataRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new MdmsDataRepositoryImpl(
                producer,
                jdbcTemplate,
                applicationConfig,
                mdmsDataQueryBuilder,
                mdmsDataRowMapperV2,
                mdmsDataQueryBuilderV2,
                mdmsDataRowMapper
        );
    }

    @Test
    void create_pushesToSaveTopic() {
        when(applicationConfig.getSaveMdmsDataTopicName()).thenReturn("save-mdms");
        MdmsRequest request = MdmsRequest.builder().build();

        repository.create(request);

        verify(producer).push("save-mdms", request);
    }

    @Test
    void update_pushesToUpdateTopic() {
        when(applicationConfig.getUpdateMdmsDataTopicName()).thenReturn("update-mdms");
        MdmsRequest request = MdmsRequest.builder().build();

        repository.update(request);

        verify(producer).push("update-mdms", request);
    }

    @Test
    void delete_pushesToDeleteTopic() {
        when(applicationConfig.getDeleteMdmsDataTopicName()).thenReturn("delete-mdms");
        MdmsRequest request = MdmsRequest.builder().build();

        repository.delete(request);

        verify(producer).push("delete-mdms", request);
    }

    @Test
    void getById_emptySearch_returnsNull() {
        MdmsCriteriaV2 criteria = MdmsCriteriaV2.builder()
                .tenantId("pg.citya")
                .ids(Collections.singleton("id-1"))
                .build();
        when(mdmsDataQueryBuilderV2.getMdmsDataSearchQuery(any(), any())).thenReturn("SELECT 1");
        when(jdbcTemplate.query(eq("SELECT 1"), any(Object[].class), eq(mdmsDataRowMapperV2)))
                .thenReturn(Collections.emptyList());

        assertThat(repository.getById("pg.citya", "id-1")).isNull();
    }

    @Test
    void getById_found_returnsFirstRow() {
        Mdms mdms = Mdms.builder().id("id-1").tenantId("pg.citya").build();
        when(mdmsDataQueryBuilderV2.getMdmsDataSearchQuery(any(), any())).thenReturn("SELECT 1");
        when(jdbcTemplate.query(eq("SELECT 1"), any(Object[].class), eq(mdmsDataRowMapperV2)))
                .thenReturn(List.of(mdms));

        assertThat(repository.getById("pg.citya", "id-1")).isSameAs(mdms);
    }

    @Test
    void searchV2_delegatesToJdbcTemplate() {
        MdmsCriteriaV2 criteria = MdmsCriteriaV2.builder().tenantId("pg.citya").build();
        when(mdmsDataQueryBuilderV2.getMdmsDataSearchQuery(eq(criteria), any())).thenReturn("SELECT v2");
        when(jdbcTemplate.query(eq("SELECT v2"), any(Object[].class), eq(mdmsDataRowMapperV2)))
                .thenReturn(List.of(Mdms.builder().id("a").build()));

        List<Mdms> result = repository.searchV2(criteria);

        assertThat(result).hasSize(1);
        verify(mdmsDataQueryBuilderV2).getMdmsDataSearchQuery(eq(criteria), any());
    }

    @Test
    void search_delegatesToJdbcTemplate() {
        MdmsCriteria criteria = MdmsCriteria.builder().tenantId("pg.citya").build();
        ArgumentCaptor<List<Object>> paramCaptor = ArgumentCaptor.forClass(List.class);
        when(mdmsDataQueryBuilder.getMdmsDataSearchQuery(eq(criteria), paramCaptor.capture()))
                .thenReturn("SELECT v1");
        Map<String, Map<String, JSONArray>> expected = new HashMap<>();
        when(jdbcTemplate.query(eq("SELECT v1"), any(Object[].class), eq(mdmsDataRowMapper)))
                .thenReturn(expected);

        assertThat(repository.search(criteria)).isSameAs(expected);
    }
}
