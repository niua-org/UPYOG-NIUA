package org.egov.tenant.persistence.repository;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tenant.web.contract.MdmsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

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
class MdmsRepositoryTest {

    @Mock
    private RestTemplate restTemplate;

    private MdmsRepository mdmsRepository;

    @BeforeEach
    void setUp() {
        mdmsRepository = new MdmsRepository(restTemplate, "http://mdms", "/egov-mdms-service/v1/_search");
    }

    @Test
    void getByCriteria_validResponse_returnsMasterArray() {
        JSONArray tenants = new JSONArray();
        tenants.add(new JSONObject());
        Map<String, Map<String, JSONArray>> mdmsRes = new HashMap<>();
        mdmsRes.put("tenant", Map.of("City", tenants));
        MdmsResponse response = MdmsResponse.builder().mdmsRes(mdmsRes).build();
        when(restTemplate.postForObject(eq("http://mdms/egov-mdms-service/v1/_search"), any(), eq(MdmsResponse.class)))
                .thenReturn(response);

        RequestInfo requestInfo = RequestInfo.builder().apiId("tenant").action("search").ts(1L).build();
        JSONArray result = mdmsRepository.getByCriteria("pb", "tenant", "City", "code",
                List.of("pb.amritsar"), requestInfo);

        assertThat(result).isSameAs(tenants);
        ArgumentCaptor<Object> requestCaptor = ArgumentCaptor.forClass(Object.class);
        verify(restTemplate).postForObject(eq("http://mdms/egov-mdms-service/v1/_search"), requestCaptor.capture(),
                eq(MdmsResponse.class));
    }

    @Test
    void getByCriteria_nullResponse_returnsEmptyArray() {
        when(restTemplate.postForObject(eq("http://mdms/egov-mdms-service/v1/_search"), any(), eq(MdmsResponse.class)))
                .thenReturn(null);

        JSONArray result = mdmsRepository.getByCriteria("pb", "tenant", "City", "code",
                Collections.emptyList(), RequestInfo.builder().build());

        assertThat(result).isEmpty();
    }

    @Test
    void getByCriteria_missingMasterKey_returnsEmptyArray() {
        MdmsResponse response = MdmsResponse.builder().mdmsRes(Map.of("tenant", Map.of())).build();
        when(restTemplate.postForObject(eq("http://mdms/egov-mdms-service/v1/_search"), any(), eq(MdmsResponse.class)))
                .thenReturn(response);

        JSONArray result = mdmsRepository.getByCriteria("pb", "tenant", "City", "code", null,
                RequestInfo.builder().build());

        assertThat(result).isEmpty();
    }
}
