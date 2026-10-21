package org.egov.infra.mdms.service.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.infra.mdms.model.Mdms;
import org.egov.infra.mdms.model.MdmsCriteriaV2;
import org.egov.infra.mdms.model.MdmsRequest;
import org.egov.infra.mdms.repository.MdmsDataRepository;
import org.egov.infra.mdms.service.SchemaDefinitionService;
import org.egov.infra.mdms.utils.CompositeUniqueIdentifierGenerationUtil;
import org.egov.tracer.model.CustomException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MdmsDataValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String UNIQUE_KEY = "x-unique";
    private static final String REFERENCE_KEY = "x-ref-schema";
    private static final String FIELD_PATH = "fieldPath";
    private static final String SCHEMA_CODE_KEY = "schemaCode";

    @Mock
    private SchemaDefinitionService schemaDefinitionService;

    @Mock
    private MdmsDataRepository mdmsDataRepository;

    @InjectMocks
    private MdmsDataValidator validator;

    @ParameterizedTest
    @MethodSource("absentMasters")
    void validateCreateRequest_noExistingRows_doesNotThrow(List<Mdms> existing) {
        when(mdmsDataRepository.searchV2(any())).thenReturn(existing);

        assertThatCode(() -> validator.validateCreateRequest(requestWith("id-1", "{\"code\":\"A\"}"), masterSchema()))
                .doesNotThrowAnyException();
    }

    @Test
    void validateCreateRequest_duplicateRecord_throwsCustomException() {
        when(mdmsDataRepository.searchV2(any())).thenReturn(Collections.singletonList(mock(Mdms.class)));

        Throwable thrown = catchThrowable(() ->
                validator.validateCreateRequest(requestWith("id-1", "{\"code\":\"A\"}"), masterSchema()));

        assertCustomExceptionContains(thrown, "Duplicate");
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("schemaRuleViolations")
    void validateCreateRequest_schemaRuleViolated_throwsCustomException(String data, String snippet) {
        when(mdmsDataRepository.searchV2(any())).thenReturn(Collections.emptyList());

        Throwable thrown = catchThrowable(() ->
                validator.validateCreateRequest(requestWith("id-1", data), masterSchema()));

        assertCustomExceptionContains(thrown, snippet);
    }

    @Test
    void validateCreateRequest_multipleSchemaViolations_throwsCustomException() {
        when(mdmsDataRepository.searchV2(any())).thenReturn(Collections.emptyList());

        Throwable thrown = catchThrowable(() -> validator.validateCreateRequest(
                requestWith("id-1", "{\"code\":\"\",\"name\":1}"), masterSchema()));

        assertThat(thrown).isInstanceOf(CustomException.class);
        assertThat(textOf(thrown).toLowerCase(Locale.ROOT)).contains("minlength");
    }

    @Test
    void validateCreateRequest_nullRequest_throwsCustomException() {
        Throwable thrown = catchThrowable(() -> validator.validateCreateRequest(null, masterSchema()));

        assertCustomExceptionContains(thrown, "unknown error");
    }

    @Test
    void validateCreateRequest_nullMdms_throwsCustomException() {
        MdmsRequest request = mock(MdmsRequest.class);
        when(request.getMdms()).thenReturn(null);

        Throwable thrown = catchThrowable(() -> validator.validateCreateRequest(request, masterSchema()));

        assertCustomExceptionContains(thrown, "unknown error");
    }

    @Test
    void validateCreateRequest_nullData_throwsCustomException() {
        Throwable thrown = catchThrowable(() ->
                validator.validateCreateRequest(requestWith("id-1", null), masterSchema()));

        assertCustomExceptionContains(thrown, "unknown error");
    }

    @Test
    void validateCreateRequest_nullSchema_throwsCustomException() {
        Throwable thrown = catchThrowable(() ->
                validator.validateCreateRequest(requestWith("id-1", "{\"code\":\"A\"}"), null));

        assertCustomExceptionContains(thrown, "unknown error");
    }

    @Test
    void validateCreateRequest_emptySchema_throwsRuntimeException() {
        Throwable thrown = catchThrowable(() ->
                validator.validateCreateRequest(requestWith("id-1", "{\"code\":\"A\"}"), new JSONObject()));

        assertThat(thrown).isInstanceOf(RuntimeException.class);
    }

    @Test
    void validateCreateRequest_emptyReferenceList_doesNotThrow() {
        when(mdmsDataRepository.searchV2(any())).thenReturn(Collections.emptyList());
        JSONObject schema = masterSchema();
        schema.put(REFERENCE_KEY, new JSONArray());

        assertThatCode(() -> validator.validateCreateRequest(requestWith("id-1", "{\"code\":\"A\"}"), schema))
                .doesNotThrowAnyException();
    }

    @Test
    void validateCreateRequest_unknownReference_throwsCustomException() {
        when(mdmsDataRepository.searchV2(any())).thenReturn(Collections.emptyList());

        Throwable thrown = catchThrowable(() -> validator.validateCreateRequest(
                requestWith("id-1", "{\"code\":\"A\",\"ref\":\"MISSING\"}"), schemaWithReference()));

        assertCustomExceptionContains(thrown, "does not exist");
    }

    @Test
    void validateCreateRequest_referenceObject_throwsCustomException() {
        when(mdmsDataRepository.searchV2(any())).thenReturn(Collections.emptyList());

        Throwable thrown = catchThrowable(() -> validator.validateCreateRequest(
                requestWith("id-1", "{\"code\":\"A\",\"ref\":{\"id\":\"x\"}}"), schemaWithReference()));

        assertCustomExceptionContains(thrown, "string, number");
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"R1\"", "10", "[\"R1\"]"})
    void validateCreateRequest_supportedReferenceFound_doesNotThrow(String refJson) {
        when(mdmsDataRepository.searchV2(any())).thenAnswer(invocation -> {
            MdmsCriteriaV2 criteria = invocation.getArgument(0);
            Set<String> refs = criteria.getUniqueIdentifiersForRefVerification();
            if (refs != null && !refs.isEmpty()) {
                List<Mdms> found = new ArrayList<Mdms>();
                for (int i = 0; i < refs.size(); i++) {
                    found.add(mock(Mdms.class));
                }
                return found;
            }
            return Collections.emptyList();
        });

        assertThatCode(() -> validator.validateCreateRequest(
                requestWith("id-1", "{\"code\":\"A\",\"ref\":" + refJson + "}"), schemaWithReference()))
                .doesNotThrowAnyException();
    }

    @Test
    void validateUpdateRequest_nullRequest_throwsNullPointerException() {
        assertThat(catchThrowable(() -> validator.validateUpdateRequest(null, masterSchema())))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void validateUpdateRequest_nullMdms_throwsNullPointerException() {
        MdmsRequest request = mock(MdmsRequest.class);
        when(request.getMdms()).thenReturn(null);

        assertThat(catchThrowable(() -> validator.validateUpdateRequest(request, masterSchema())))
                .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void validateUpdateRequest_idNullOrEmpty_throwsCustomException(String id) {
        Throwable thrown = catchThrowable(() ->
                validator.validateUpdateRequest(requestWith(id, "{\"code\":\"A\"}"), masterSchema()));

        assertCustomExceptionContains(thrown, "mandatory");
    }

    @Test
    void validateUpdateRequest_whitespaceId_throwsMasterDataNotFound() {
        when(mdmsDataRepository.searchV2(any())).thenReturn(Collections.emptyList());

        Throwable thrown = catchThrowable(() ->
                validator.validateUpdateRequest(requestWith(" ", "{\"code\":\"A\"}"), masterSchema()));

        assertCustomExceptionContains(thrown, "not found");
    }

    @ParameterizedTest
    @MethodSource("absentMasters")
    void validateUpdateRequest_masterNotFound_throwsCustomException(List<Mdms> existing) {
        when(mdmsDataRepository.searchV2(any())).thenReturn(existing);

        Throwable thrown = catchThrowable(() ->
                validator.validateUpdateRequest(requestWith("id-1", "{\"code\":\"A\"}"), masterSchema()));

        assertCustomExceptionContains(thrown, "not found");
    }

    @Test
    void validateUpdateRequest_uniqueFieldsChanged_throwsCustomException() {
        Mdms existing = mock(Mdms.class);
        when(existing.getUniqueIdentifier()).thenReturn("OLD");
        when(mdmsDataRepository.searchV2(any())).thenReturn(Collections.singletonList(existing));

        Throwable thrown = catchThrowable(() ->
                validator.validateUpdateRequest(requestWith("id-1", "{\"code\":\"NEW\"}"), masterSchema()));

        assertCustomExceptionContains(thrown, "unique");
    }

    @Test
    void validateUpdateRequest_existingUniqueIdentifierNull_throwsNullPointerException() {
        MdmsRequest request = requestWith("id-1", "{\"code\":\"A\"}");
        Mdms existing = mock(Mdms.class);
        when(existing.getUniqueIdentifier()).thenReturn(null);
        when(mdmsDataRepository.searchV2(any())).thenReturn(Collections.singletonList(existing));

        assertThat(catchThrowable(() -> validator.validateUpdateRequest(request, masterSchema())))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void validateUpdateRequest_uniqueIdentifierDiffersByCase_doesNotThrow() {
        MdmsRequest request = requestWith("id-1", "{\"code\":\"Ab\"}");
        JSONObject schema = masterSchema();
        String uid = CompositeUniqueIdentifierGenerationUtil.getUniqueIdentifier(schema, request);
        Mdms existing = mock(Mdms.class);
        when(existing.getUniqueIdentifier()).thenReturn(uid.toUpperCase(Locale.ROOT));
        when(mdmsDataRepository.searchV2(any())).thenReturn(Collections.singletonList(existing));

        assertThatCode(() -> validator.validateUpdateRequest(request, schema)).doesNotThrowAnyException();
    }

    private static Stream<Arguments> absentMasters() {
        return Stream.of(
                Arguments.of(Collections.emptyList()),
                Arguments.of((Object) null)
        );
    }

    private static Stream<Arguments> schemaRuleViolations() {
        return Stream.of(
                Arguments.of("{\"code\":\"\"}", "expected minLength"),
                Arguments.of("{\"code\":\"A\",\"name\":1}", "expected type")
        );
    }

    private static JSONObject masterSchema() {
        JSONObject code = new JSONObject();
        code.put("type", "string");
        code.put("minLength", 1);

        JSONObject name = new JSONObject();
        name.put("type", "string");

        JSONObject properties = new JSONObject();
        properties.put("code", code);
        properties.put("name", name);

        JSONObject schema = new JSONObject();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", new JSONArray().put("code"));
        schema.put(UNIQUE_KEY, new JSONArray().put("code"));
        return schema;
    }

    private static JSONObject schemaWithReference() {
        JSONObject schema = masterSchema();
        JSONObject ref = new JSONObject();
        ref.put(FIELD_PATH, "ref");
        ref.put(SCHEMA_CODE_KEY, "common-masters.Ref");
        schema.put(REFERENCE_KEY, new JSONArray().put(ref));
        return schema;
    }

    private static MdmsRequest requestWith(String id, String data) {
        Mdms mdms = mock(Mdms.class);
        when(mdms.getId()).thenReturn(id);
        when(mdms.getTenantId()).thenReturn("pg.citya");
        when(mdms.getSchemaCode()).thenReturn("common-masters.Department");
        if (data != null) {
            when(mdms.getData()).thenReturn(read(data));
        }
        MdmsRequest request = mock(MdmsRequest.class);
        when(request.getMdms()).thenReturn(mdms);
        return request;
    }

    private static JsonNode read(String raw) {
        try {
            return MAPPER.readTree(raw);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void assertCustomExceptionContains(Throwable thrown, String snippet) {
        assertThat(thrown).isInstanceOf(CustomException.class);
        assertThat(textOf(thrown).toLowerCase(Locale.ROOT)).contains(snippet.toLowerCase(Locale.ROOT));
    }

    private static String textOf(Throwable thrown) {
        return String.valueOf(thrown.getMessage()) + " " + thrown;
    }
}
