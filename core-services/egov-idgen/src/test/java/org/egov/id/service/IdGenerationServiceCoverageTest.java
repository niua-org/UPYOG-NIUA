package org.egov.id.service;

import org.egov.id.config.PropertiesManager;
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
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdGenerationServiceCoverageTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private MdmsService mdmsService;

    @Mock
    private PropertiesManager propertiesManager;

    private IdGenerationService service;

    @BeforeEach
    void setUp() {
        service = new IdGenerationService();
        service.idFormatFromMDMS = false;
        service.autoCreateNewSeq = false;
        setField("jdbcTemplate", jdbcTemplate);
        setField("responseInfoFactory", new ResponseInfoFactory());
        setField("mdmsService", mdmsService);
        setField("propertiesManager", propertiesManager);
    }

    @Test
    void generateIdResponse_sequenceFormat_returnsPaddedId() throws Exception {
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class), eq(String.class)))
                .thenReturn(List.of("7"));

        IdGenerationResponse response = service.generateIdResponse(
                requestWith(new IdRequest(null, "pb.amritsar", "REC/[seq_receipt]", 1)));

        assertThat(response.getIdResponses()).hasSize(1);
        assertThat(response.getIdResponses().get(0).getId()).isEqualTo("REC/000007");
    }

    @Test
    void generateIdResponse_tenantPlaceholders_replacesTenantTokens() throws Exception {
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class), eq(String.class)))
                .thenReturn(List.of("1"));

        IdGenerationResponse response = service.generateIdResponse(requestWith(new IdRequest(
                null,
                "pb.amritsar",
                "[tenantid]-[tenant_id]-[TENANT_ID]/[seq_x]",
                1)));

        assertThat(response.getIdResponses().get(0).getId())
                .isEqualTo("pb.amritsar-pb_amritsar-PB_AMRITSAR/000001");
    }

    @Test
    void generateIdResponse_currentYearFormat_usesConfiguredTimezone() throws Exception {
        when(propertiesManager.getTimeZone()).thenReturn("UTC");
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class), eq(String.class)))
                .thenReturn(List.of("1"));

        IdGenerationResponse response = service.generateIdResponse(
                requestWith(new IdRequest(null, "pb", "FY[cy:yyyy]/[seq_x]", 1)));

        String id = response.getIdResponses().get(0).getId();
        assertThat(id).startsWith("FY20");
        assertThat(id).endsWith("/000001");
    }

    @Test
    void generateIdResponse_financialYearFormat_returnsYearRange() throws Exception {
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class), eq(String.class)))
                .thenReturn(List.of("1"));

        IdGenerationResponse response = service.generateIdResponse(
                requestWith(new IdRequest(null, "pb", "R/[fy:yyyy-yyyy]/[seq_x]", 1)));

        assertThat(response.getIdResponses().get(0).getId()).matches("R/\\d{4}-\\d{4}/000001");
    }

    @Test
    void generateIdResponse_cityToken_usesMdmsCityCode() throws Exception {
        when(mdmsService.getCity(any(RequestInfo.class), any(IdRequest.class))).thenReturn("AMR");
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class), eq(String.class)))
                .thenReturn(List.of("1"));

        IdGenerationResponse response = service.generateIdResponse(
                requestWith(new IdRequest(null, "pb.amritsar", "C/[city:code]/[seq_x]", 1)));

        assertThat(response.getIdResponses().get(0).getId()).isEqualTo("C/AMR/000001");
    }

    @Test
    void generateIdResponse_randomToken_generatesDigitsFromPattern() throws Exception {
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class), eq(String.class)))
                .thenReturn(List.of("1"));

        IdGenerationResponse response = service.generateIdResponse(
                requestWith(new IdRequest(null, "pb", "R/[\\d{3}]/[seq_x]", 1)));

        assertThat(response.getIdResponses().get(0).getId()).matches("R/\\d{3}/000001");
    }

    @Test
    void generateIdResponse_invalidRandomRegex_throwsInvalidRegex() {
        IdGenerationRequest request = requestWith(new IdRequest(null, "pb", "[?invalid]", 1));

        assertThatThrownBy(() -> service.generateIdResponse(request))
                .isInstanceOf(CustomException.class)
                .satisfies(thrown -> {
                    CustomException ex = (CustomException) thrown;
                    assertThat(ex.getCode()).isEqualTo("INVALID_REGEX");
                });
    }

    @Test
    void generateIdResponse_countGreaterThanOne_generatesMultipleIds() throws Exception {
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class), eq(String.class)))
                .thenReturn(List.of("1", "2"));

        IdGenerationResponse response = service.generateIdResponse(
                requestWith(new IdRequest(null, "pb", "[seq_batch]", 2)));

        assertThat(response.getIdResponses()).hasSize(2);
        assertThat(response.getIdResponses().get(0).getId()).isEqualTo("000001");
        assertThat(response.getIdResponses().get(1).getId()).isEqualTo("000002");
    }

    @Test
    void generateIdResponse_idNameLoadsFormatFromDb_usesDbFormat() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(String.class)))
                .thenReturn("DB/[seq_from_db]");
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class), eq(String.class)))
                .thenReturn(List.of("9"));

        IdGenerationResponse response = service.generateIdResponse(
                requestWith(new IdRequest("receipt", "pb.amritsar", null, 1)));

        assertThat(response.getIdResponses().get(0).getId()).isEqualTo("DB/000009");
    }

    @Test
    void generateIdResponse_idNameLoadsFormatFromMdms_usesMdmsFormat() throws Exception {
        service.idFormatFromMDMS = true;
        when(mdmsService.getIdFormat(any(RequestInfo.class), any(IdRequest.class)))
                .thenReturn("MDMS/[seq_mdms]");
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class), eq(String.class)))
                .thenReturn(List.of("3"));

        IdGenerationResponse response = service.generateIdResponse(
                requestWith(new IdRequest("receipt", "pb.amritsar", null, 1)));

        assertThat(response.getIdResponses().get(0).getId()).isEqualTo("MDMS/000003");
    }

    @Test
    void generateIdResponse_autoCreateSequence_createsAndRetries() throws Exception {
        service.autoCreateNewSeq = true;
        when(mdmsService.getIdFormat(any(RequestInfo.class), any(IdRequest.class)))
                .thenReturn("[seq_new]");
        service.idFormatFromMDMS = true;

        SQLException sqlException = new SQLException("undefined table", "42P01");
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class), eq(String.class)))
                .thenThrow(new BadSqlGrammarException("nextval", "SELECT NEXTVAL", sqlException))
                .thenReturn(List.of("4"));

        IdGenerationResponse response = service.generateIdResponse(
                requestWith(new IdRequest("receipt", "pb.amritsar", null, 1)));

        assertThat(response.getIdResponses().get(0).getId()).isEqualTo("000004");
        verify(jdbcTemplate).execute("CREATE SEQUENCE seq_new");
    }

    @Test
    void generateIdResponse_sequenceMissingWhenAutoCreateDisabled_throwsErrorCreatingSeq() {
        SQLException sqlException = new SQLException("undefined table", "42P01");
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class), eq(String.class)))
                .thenThrow(new BadSqlGrammarException("nextval", "SELECT NEXTVAL", sqlException));

        IdGenerationRequest request = requestWith(new IdRequest(null, "pb", "[seq_missing]", 1));

        assertThatThrownBy(() -> service.generateIdResponse(request))
                .isInstanceOf(CustomException.class)
                .satisfies(thrown -> assertThat(((CustomException) thrown).getCode()).isEqualTo("ERROR_CREATING_SEQ"));
    }

    @Test
    void generateIdResponse_nonMissingTableSqlError_throwsSeqNumberError() {
        SQLException sqlException = new SQLException("syntax error", "42601");
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class), eq(String.class)))
                .thenThrow(new BadSqlGrammarException("nextval", "SELECT NEXTVAL", sqlException));

        IdGenerationRequest request = requestWith(new IdRequest(null, "pb", "[seq_x]", 1));

        assertThatThrownBy(() -> service.generateIdResponse(request))
                .isInstanceOf(CustomException.class)
                .satisfies(thrown -> assertThat(((CustomException) thrown).getCode()).isEqualTo("SEQ_NUMBER_ERROR"));
    }

    @Test
    void generateIdResponse_dbFormatLookupFailure_returnsNullAndThrowsIdNotFound() {
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(String.class)))
                .thenThrow(new EmptyResultDataAccessException(1));

        IdGenerationRequest request = requestWith(new IdRequest("receipt", "pb.amritsar", null, 1));

        assertThatThrownBy(() -> service.generateIdResponse(request))
                .isInstanceOf(CustomException.class)
                .satisfies(thrown -> assertThat(((CustomException) thrown).getCode()).isEqualTo("ID_NOT_FOUND"));
    }

    @Test
    void generateIdResponse_genericJdbcFailureDuringSequence_throwsSeqNumberError() {
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class), eq(String.class)))
                .thenThrow(new IllegalStateException("db down"));

        IdGenerationRequest request = requestWith(new IdRequest(null, "pb", "[seq_x]", 1));

        assertThatThrownBy(() -> service.generateIdResponse(request))
                .isInstanceOf(CustomException.class)
                .satisfies(thrown -> assertThat(((CustomException) thrown).getCode()).isEqualTo("SEQ_NUMBER_ERROR"));
    }

    @Test
    void generateIdResponse_createSequenceFailure_wrapsErrorCreatingSeq() {
        service.autoCreateNewSeq = true;
        service.idFormatFromMDMS = true;
        when(mdmsService.getIdFormat(any(RequestInfo.class), any(IdRequest.class)))
                .thenReturn("[seq_fail]");
        SQLException sqlException = new SQLException("undefined table", "42P01");
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class), eq(String.class)))
                .thenThrow(new BadSqlGrammarException("nextval", "SELECT NEXTVAL", sqlException));
        doThrow(new IllegalStateException("cannot create")).when(jdbcTemplate).execute(anyString());

        IdGenerationRequest request = requestWith(new IdRequest("receipt", "pb.amritsar", null, 1));

        assertThatThrownBy(() -> service.generateIdResponse(request))
                .isInstanceOf(CustomException.class)
                .satisfies(thrown -> assertThat(((CustomException) thrown).getCode()).isEqualTo("ERROR_CREATING_SEQ"));
    }

    private static IdGenerationRequest requestWith(IdRequest idRequest) {
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
