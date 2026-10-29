package org.egov.infra.mdms.controller;

import org.egov.common.contract.request.RequestInfo;
import org.egov.infra.mdms.model.SchemaDefCriteria;
import org.egov.infra.mdms.model.SchemaDefSearchRequest;
import org.egov.infra.mdms.model.SchemaDefinition;
import org.egov.infra.mdms.model.SchemaDefinitionRequest;
import org.egov.infra.mdms.model.SchemaDefinitionResponse;
import org.egov.infra.mdms.model.SchemaDeleteRequest;
import org.egov.infra.mdms.repository.SchemaDefinitionRepository;
import org.egov.infra.mdms.service.SchemaDefinitionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchemaDefinitionControllerTest {

    @Mock
    private SchemaDefinitionService schemaDefinitionService;
    @Mock
    private SchemaDefinitionRepository schemaDefinitionRepository;

    @InjectMocks
    private SchemaDefinitionController controller;

    @Test
    void create_returnsAcceptedWithDefinitions() {
        SchemaDefinitionRequest request = SchemaDefinitionRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .schemaDefinition(SchemaDefinition.builder().code("common.StateInfo").build())
                .build();
        when(schemaDefinitionService.create(request))
                .thenReturn(List.of(request.getSchemaDefinition()));

        ResponseEntity<SchemaDefinitionResponse> response = controller.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody().getSchemaDefinitions()).hasSize(1);
    }

    @Test
    void search_includesTotalMasterCount() {
        SchemaDefSearchRequest request = SchemaDefSearchRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .schemaDefCriteria(SchemaDefCriteria.builder().tenantId("pg").build())
                .build();
        when(schemaDefinitionService.search(request)).thenReturn(List.of());
        when(schemaDefinitionRepository.getTotalMastersCount("pg")).thenReturn(3);

        ResponseEntity<SchemaDefinitionResponse> response = controller.search(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody().getTotalMasters()).isEqualTo(3);
    }

    @Test
    void delete_returnsOk() {
        SchemaDeleteRequest request = SchemaDeleteRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .tenantId("pg")
                .code("common.StateInfo")
                .build();
        when(schemaDefinitionService.delete(request))
                .thenReturn(List.of(SchemaDefinition.builder().code("common.StateInfo").build()));

        ResponseEntity<SchemaDefinitionResponse> response = controller.delete(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getSchemaDefinitions()).hasSize(1);
    }

    @Test
    void update_notImplemented() {
        ResponseEntity<SchemaDefinitionResponse> response =
                controller.update(SchemaDefinitionRequest.builder().build());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
    }
}
