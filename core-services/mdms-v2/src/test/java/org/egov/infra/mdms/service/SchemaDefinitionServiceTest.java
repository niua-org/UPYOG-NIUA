package org.egov.infra.mdms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.infra.mdms.config.ApplicationConfig;
import org.egov.infra.mdms.model.SchemaDefCriteria;
import org.egov.infra.mdms.model.SchemaDefSearchRequest;
import org.egov.infra.mdms.model.SchemaDefinition;
import org.egov.infra.mdms.model.SchemaDefinitionRequest;
import org.egov.infra.mdms.model.SchemaDeleteRequest;
import org.egov.infra.mdms.repository.SchemaDefinitionRepository;
import org.egov.infra.mdms.service.enrichment.SchemaDefinitionEnricher;
import org.egov.infra.mdms.service.validator.SchemaDefinitionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchemaDefinitionServiceTest {

    @Mock
    private SchemaDefinitionRepository schemaDefinitionRepository;
    @Mock
    private ApplicationConfig applicationConfig;
    @Mock
    private SchemaDefinitionEnricher schemaDefinitionEnricher;
    @Mock
    private SchemaDefinitionValidator schemaDefinitionValidator;

    private SchemaDefinitionService service;

    @BeforeEach
    void setUp() {
        service = new SchemaDefinitionService(
                schemaDefinitionRepository, applicationConfig, schemaDefinitionEnricher, schemaDefinitionValidator);
    }

    @Test
    void create_validatesEnrichesAndPublishes() {
        SchemaDefinition definition = SchemaDefinition.builder().tenantId("pg.citya").code("common.StateInfo").build();
        SchemaDefinitionRequest request = SchemaDefinitionRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .schemaDefinition(definition)
                .build();
        doNothing().when(schemaDefinitionValidator).validateCreateRequest(request);
        doNothing().when(schemaDefinitionEnricher).enrichCreateRequest(request);

        List<SchemaDefinition> result = service.create(request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOperation()).isEqualTo("CREATE");
        verify(schemaDefinitionRepository).create(request);
    }

    @Test
    void delete_missingSchema_throws() {
        SchemaDeleteRequest request = SchemaDeleteRequest.builder()
                .tenantId("pg")
                .code("missing.code")
                .build();
        when(schemaDefinitionRepository.search(any(SchemaDefCriteria.class))).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.delete(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void delete_existingSchema_publishesDelete() {
        SchemaDeleteRequest request = SchemaDeleteRequest.builder()
                .tenantId("pg")
                .code("common.StateInfo")
                .build();
        when(schemaDefinitionRepository.search(any(SchemaDefCriteria.class)))
                .thenReturn(List.of(SchemaDefinition.builder().code("common.StateInfo").build()));

        List<SchemaDefinition> deleted = service.delete(request);

        assertThat(deleted.get(0).getCode()).isEqualTo("common.StateInfo");
        verify(schemaDefinitionRepository).delete(request);
    }

    @Test
    void search_getAllCodes_collapsesByModulePrefix() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        SchemaDefinition a = SchemaDefinition.builder().code("common.StateInfo").definition(mapper.readTree("{}")).build();
        SchemaDefinition b = SchemaDefinition.builder().code("common.City").definition(mapper.readTree("{}")).build();
        SchemaDefSearchRequest searchRequest = SchemaDefSearchRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .schemaDefCriteria(SchemaDefCriteria.builder()
                        .tenantId("pg")
                        .isGetAllCodes(true)
                        .build())
                .build();
        when(schemaDefinitionRepository.search(searchRequest.getSchemaDefCriteria())).thenReturn(List.of(a, b));

        List<SchemaDefinition> result = service.search(searchRequest);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("common");
        assertThat(result.get(0).getDefinition()).isNull();
    }

    @Test
    void search_regularCriteria_returnsRepositoryRows() {
        SchemaDefSearchRequest searchRequest = SchemaDefSearchRequest.builder()
                .schemaDefCriteria(SchemaDefCriteria.builder().tenantId("pg").build())
                .build();
        List<SchemaDefinition> rows = List.of(SchemaDefinition.builder().code("common.StateInfo").build());
        when(schemaDefinitionRepository.search(searchRequest.getSchemaDefCriteria())).thenReturn(rows);

        assertThat(service.search(searchRequest)).isEqualTo(rows);
    }
}
