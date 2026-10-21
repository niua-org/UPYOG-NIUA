package org.egov.id.service;

import org.egov.id.model.IdGenerationRequest;
import org.egov.id.model.IdGenerationResponse;
import org.egov.id.model.IdRequest;
import org.egov.id.model.RequestInfo;
import org.egov.id.model.ResponseInfoFactory;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdGenerationServiceBranchesTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private MdmsService mdmsService;

    private IdGenerationService service;

    @BeforeEach
    void setUp() {
        service = new IdGenerationService();
        service.idFormatFromMDMS = false;
        service.autoCreateNewSeq = false;
        setField("jdbcTemplate", jdbcTemplate);
        setField("responseInfoFactory", new ResponseInfoFactory());
        setField("mdmsService", mdmsService);
    }

    @Test
    void generateIdResponse_nullRequest_throwsNullPointerException() {
        assertThatThrownBy(() -> service.generateIdResponse(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void generateIdResponse_nullIdRequests_throwsNullPointerException() {
        IdGenerationRequest request = new IdGenerationRequest();
        request.setRequestInfo(new RequestInfo());
        request.setIdRequests(null);

        assertThatThrownBy(() -> service.generateIdResponse(request))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void generateIdResponse_emptyIdRequests_returnsResponseWithoutIds() throws Exception {
        IdGenerationRequest request = new IdGenerationRequest();
        request.setRequestInfo(new RequestInfo());
        request.setIdRequests(Collections.emptyList());

        IdGenerationResponse response = service.generateIdResponse(request);

        assertThat(response).isNotNull();
        assertThat(response.getResponseInfo()).isNotNull();
        assertThat(response.getIdResponses()).isNull();
    }

    @Test
    void generateIdResponse_sequenceNotFound_throwsSequenceCreationError() {
        SQLException sqlException = new SQLException("relation does not exist", "42P01");
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class), eq(String.class)))
                .thenThrow(new BadSqlGrammarException("nextval", "SELECT NEXTVAL ('seq_application_id')", sqlException));

        IdGenerationRequest request = requestWith(new IdRequest(null, "pb.amritsar", "[seq_application_id]", 1));

        assertThatThrownBy(() -> service.generateIdResponse(request))
                .isInstanceOf(CustomException.class)
                .satisfies(thrown -> {
                    CustomException exception = (CustomException) thrown;
                    assertThat(exception.getCode()).isEqualTo("ERROR_CREATING_SEQ");
                    assertThat(exception.getMessage()).isEqualTo("Error occurred while auto creating seq in DB");
                });
    }

    @Test
    void generateIdResponse_sequenceAboveIntegerMax_throwsNumberFormatException() {
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class), eq(String.class)))
                .thenReturn(List.of("2147483648"));

        IdGenerationRequest request = requestWith(new IdRequest(null, "pb.amritsar", "[seq_application_id]", 1));

        assertThatThrownBy(() -> service.generateIdResponse(request))
                .isInstanceOf(NumberFormatException.class);
    }

    @Test
    void generateIdResponse_invalidFinancialYearFormat_throwsInvalidFormat() {
        IdGenerationRequest request = requestWith(new IdRequest(null, "pb.amritsar", "[fy:qqqq]", 1));

        assertThatThrownBy(() -> service.generateIdResponse(request))
                .isInstanceOf(CustomException.class)
                .satisfies(thrown -> {
                    CustomException exception = (CustomException) thrown;
                    assertThat(exception.getCode()).isEqualTo("INVALID_FORMAT");
                    assertThat(exception.getMessage()).contains("financial year");
                });
    }

    @Test
    void generateIdResponse_mdmsFormatMissing_throwsIdNotFound() throws Exception {
        service.idFormatFromMDMS = true;
        when(mdmsService.getIdFormat(any(RequestInfo.class), any(IdRequest.class)))
                .thenThrow(new IllegalStateException("mdms unavailable"));

        IdGenerationRequest request = requestWith(new IdRequest("pt.receiptid", "pb.amritsar", null, 1));

        assertThatThrownBy(() -> service.generateIdResponse(request))
                .isInstanceOf(CustomException.class)
                .satisfies(thrown -> {
                    CustomException exception = (CustomException) thrown;
                    assertThat(exception.getCode()).isEqualTo("ID_NOT_FOUND");
                });
    }

    private IdGenerationRequest requestWith(IdRequest idRequest) {
        IdGenerationRequest request = new IdGenerationRequest();
        request.setRequestInfo(new RequestInfo());
        request.setIdRequests(List.of(idRequest));
        return request;
    }

    private void setField(String fieldName, Object value) {
        try {
            Field field = IdGenerationService.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(service, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
