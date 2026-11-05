package org.egov.infra.mdms.service.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.infra.mdms.model.SchemaDefinition;
import org.egov.infra.mdms.model.SchemaDefinitionRequest;
import org.egov.infra.mdms.repository.SchemaDefinitionRepository;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchemaDefinitionValidatorTest {

    @Mock
    private SchemaDefinitionRepository schemaDefinitionRepository;

    @InjectMocks
    private SchemaDefinitionValidator validator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void validateCreateRequest_missingRequiredKey_throwsCustomException() throws Exception {
        SchemaDefinition definition = SchemaDefinition.builder()
                .tenantId("pg")
                .code("Department")
                .definition(objectMapper.readTree("{\"type\":\"object\",\"x-unique\":[\"code\"]}"))
                .build();
        SchemaDefinitionRequest request = SchemaDefinitionRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .schemaDefinition(definition)
                .build();
        when(schemaDefinitionRepository.search(any())).thenReturn(java.util.Collections.emptyList());

        assertThatThrownBy(() -> validator.validateCreateRequest(request))
                .isInstanceOf(CustomException.class);
    }
}
