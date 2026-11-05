package org.egov.id.service;

import org.egov.common.contract.response.ResponseInfo;
import org.egov.id.model.IdRequest;
import org.egov.id.model.RequestInfo;
import org.egov.mdms.model.MasterDetail;
import org.egov.mdms.model.MdmsResponse;
import org.egov.mdms.service.MdmsClientService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import net.minidev.json.JSONArray;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = MdmsService.class)
@ExtendWith(SpringExtension.class)
class MdmsServiceSuccessTest {

    @MockBean
    private MdmsClientService mdmsClientService;

    @Autowired
    private MdmsService mdmsService;

    @Test
    void getCity_successfulMdmsResponse_returnsCityCode() throws Exception {
        when(mdmsClientService.getMaster(any(), eq("pb.amritsar"), any()))
                .thenReturn(mdmsResponseWithCityAndFormat("AMR", "FMT/[seq_x]"));

        String city = mdmsService.getCity(new RequestInfo(), idRequest("receipt", "pb.amritsar"));

        assertThat(city).isEqualTo("AMR");
        verify(mdmsClientService).getMaster(any(), eq("pb.amritsar"), any());
    }

    @Test
    void getIdFormat_successfulMdmsResponse_returnsFormat() throws Exception {
        when(mdmsClientService.getMaster(any(), eq("pb.amritsar"), any()))
                .thenReturn(mdmsResponseWithCityAndFormat("AMR", "REC/[seq_x]"));

        String format = mdmsService.getIdFormat(new RequestInfo(), idRequest("receipt", "pb.amritsar"));

        assertThat(format).isEqualTo("REC/[seq_x]");
    }

    @Test
    void getCity_nullCityInResponse_throwsParsingError() throws Exception {
        Map<String, Map<String, JSONArray>> mdmsRes = new HashMap<>();
        mdmsRes.put("tenant", Map.of("tenants", new JSONArray()));
        when(mdmsClientService.getMaster(any(), any(), any()))
                .thenReturn(new MdmsResponse(new ResponseInfo(), mdmsRes));

        assertThatThrownBy(() -> mdmsService.getCity(new RequestInfo(), idRequest("receipt", "pb")))
                .isInstanceOf(org.egov.tracer.model.CustomException.class);
    }

    private static IdRequest idRequest(String idName, String tenantId) {
        IdRequest idRequest = new IdRequest();
        idRequest.setIdName(idName);
        idRequest.setTenantId(tenantId);
        return idRequest;
    }

    private static MdmsResponse mdmsResponseWithCityAndFormat(String cityCode, String format) {
        Map<String, Object> cityRecord = new LinkedHashMap<>();
        cityRecord.put("city", Map.of("code", cityCode));
        Map<String, Object> formatRecord = Map.of("format", format);

        JSONArray tenantRecords = new JSONArray();
        tenantRecords.add(cityRecord);
        JSONArray formatRecords = new JSONArray();
        formatRecords.add(formatRecord);

        Map<String, JSONArray> tenantModule = new HashMap<>();
        tenantModule.put("tenants", tenantRecords);
        Map<String, JSONArray> commonModule = new HashMap<>();
        commonModule.put("IdFormat", formatRecords);

        Map<String, Map<String, JSONArray>> mdmsRes = new HashMap<>();
        mdmsRes.put("tenant", tenantModule);
        mdmsRes.put("common-masters", commonModule);

        return new MdmsResponse(new ResponseInfo(), mdmsRes);
    }
}
