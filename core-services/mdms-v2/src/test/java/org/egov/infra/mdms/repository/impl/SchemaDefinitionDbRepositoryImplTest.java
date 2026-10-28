package org.egov.infra.mdms.repository.impl;

import org.egov.infra.mdms.config.ApplicationConfig;
import org.egov.infra.mdms.model.SchemaDefCriteria;
import org.egov.infra.mdms.model.SchemaDefinition;
import org.egov.infra.mdms.model.SchemaDefinitionRequest;
import org.egov.infra.mdms.model.SchemaDeleteRequest;
import org.egov.infra.mdms.producer.Producer;
import org.egov.infra.mdms.repository.querybuilder.SchemaDefinitionQueryBuilder;
import org.egov.infra.mdms.repository.rowmapper.SchemaDefinitionRowMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchemaDefinitionDbRepositoryImplTest {

    @Mock
    private Producer producer;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private ApplicationConfig applicationConfig;
    @Mock
    private SchemaDefinitionRowMapper rowMapper;
    @Mock
    private SchemaDefinitionQueryBuilder schemaDefinitionQueryBuilder;

    private SchemaDefinitionDbRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new SchemaDefinitionDbRepositoryImpl(
                producer, jdbcTemplate, applicationConfig, rowMapper, schemaDefinitionQueryBuilder);
    }

    @Test
    void create_pushesToKafkaTopic() {
        when(applicationConfig.getSaveSchemaDefinitionTopicName()).thenReturn("save-schema");
        SchemaDefinitionRequest request = SchemaDefinitionRequest.builder().build();

        repository.create(request);

        verify(producer).push("save-schema", request);
    }

    @Test
    void delete_pushesToDeleteTopic() {
        when(applicationConfig.getDeleteSchemaTopicName()).thenReturn("delete-schema");
        SchemaDeleteRequest request = SchemaDeleteRequest.builder().build();

        repository.delete(request);

        verify(producer).push("delete-schema", request);
    }

    @Test
    void search_runsQueryFromBuilder() {
        SchemaDefCriteria criteria = SchemaDefCriteria.builder().tenantId("pg").build();
        when(schemaDefinitionQueryBuilder.getSchemaSearchQuery(eq(criteria), any())).thenReturn("SELECT schema");
        List<SchemaDefinition> rows = List.of(SchemaDefinition.builder().code("common.name").build());
        when(jdbcTemplate.query(eq("SELECT schema"), any(Object[].class), eq(rowMapper))).thenReturn(rows);

        assertThat(repository.search(criteria)).isEqualTo(rows);
    }

    @Test
    void getTotalMastersCount_queriesDatabase() {
        when(jdbcTemplate.queryForObject(any(String.class), any(Object[].class), eq(Integer.class)))
                .thenReturn(7);

        assertThat(repository.getTotalMastersCount("pg")).isEqualTo(7);
    }

    @Test
    void update_isNoOp() {
        repository.update(SchemaDefinitionRequest.builder().build());
    }
}
