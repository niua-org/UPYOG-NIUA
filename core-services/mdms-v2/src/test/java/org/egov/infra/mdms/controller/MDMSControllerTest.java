package org.egov.infra.mdms.controller;

import net.minidev.json.JSONArray;
import org.egov.infra.mdms.model.MdmsCriteriaReq;
import org.egov.infra.mdms.model.MdmsResponse;
import org.egov.infra.mdms.service.MDMSService;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MDMSControllerTest {

    @Mock
    private MDMSService mdmsService;

    @InjectMocks
    private MDMSController controller;

    @Test
    void search_serviceReturnsData_returnsOk() {
        MdmsCriteriaReq body = mock(MdmsCriteriaReq.class);
        Map<String, Map<String, JSONArray>> masters = new HashMap<String, Map<String, JSONArray>>();
        masters.put("common-masters", new HashMap<String, JSONArray>());
        when(mdmsService.search(body)).thenReturn(masters);

        ResponseEntity<?> response = controller.search(body);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isInstanceOf(MdmsResponse.class);
        assertThat(((MdmsResponse) response.getBody()).getMdmsRes()).isSameAs(masters);
    }

    @Test
    void search_emptyResult_returnsOk() {
        MdmsCriteriaReq body = mock(MdmsCriteriaReq.class);
        Map<String, Map<String, JSONArray>> masters = new HashMap<String, Map<String, JSONArray>>();
        when(mdmsService.search(body)).thenReturn(masters);

        ResponseEntity<?> response = controller.search(body);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(((MdmsResponse) response.getBody()).getMdmsRes()).isEmpty();
    }

    @Test
    void search_nullResult_returnsOkWithNullMdmsRes() {
        MdmsCriteriaReq body = mock(MdmsCriteriaReq.class);
        when(mdmsService.search(body)).thenReturn(null);

        ResponseEntity<?> response = controller.search(body);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(((MdmsResponse) response.getBody()).getMdmsRes()).isNull();
    }

    @Test
    void search_nullBody_returnsOk() {
        when(mdmsService.search(null)).thenReturn(new HashMap<String, Map<String, JSONArray>>());

        ResponseEntity<?> response = controller.search(null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(((MdmsResponse) response.getBody()).getMdmsRes()).isEmpty();
    }

    @Test
    void search_serviceThrows_propagatesCustomException() {
        MdmsCriteriaReq body = mock(MdmsCriteriaReq.class);
        when(mdmsService.search(body)).thenThrow(new CustomException("SEARCH_ERR", "search failed"));

        assertThatThrownBy(() -> controller.search(body))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("search failed");
    }
}
