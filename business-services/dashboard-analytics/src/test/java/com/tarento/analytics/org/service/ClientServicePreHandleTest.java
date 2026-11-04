package com.tarento.analytics.org.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tarento.analytics.constant.Constants;
import com.tarento.analytics.dto.AggregateDto;
import com.tarento.analytics.dto.AggregateRequestDto;
import com.tarento.analytics.dto.CummulativeDataRequestDto;
import com.tarento.analytics.dto.DashboardHeaderDto;
import com.tarento.analytics.exception.AINException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServicePreHandleTest {

    @Mock
    private MdmsApiMappings mdmsApiMappings;

    private final ClientService clientService = new ClientService() {
        @Override
        public AggregateDto getAggregatedData(AggregateRequestDto req, List<com.tarento.analytics.dto.RoleDto> roles)
                throws AINException, IOException {
            return null;
        }

        @Override
        public List<DashboardHeaderDto> getHeaderData(CummulativeDataRequestDto requestDto,
                List<com.tarento.analytics.dto.RoleDto> roles) throws AINException {
            return null;
        }
    };

    @Test
    void preHandle_translatesStringTenantIdWhenEnabled() {
        ObjectNode chartNode = JsonNodeFactory.instance.objectNode();
        chartNode.put(Constants.JsonPaths.TRANSLATE_CODE, true);
        AggregateRequestDto request = new AggregateRequestDto();
        request.setFilters(new HashMap<>(java.util.Map.of(Constants.MDMSKeys.TENANT_ID, "code-a")));
        when(mdmsApiMappings.valueOf("code-a")).thenReturn("resolved-a");

        boolean translateFlag = clientService.preHandle(request, chartNode, mdmsApiMappings);

        assertThat(translateFlag).isTrue();
        assertThat(request.getFilters().get(Constants.MDMSKeys.TENANT_ID)).isEqualTo("resolved-a");
    }

    @Test
    void preHandle_translatesListTenantIdsWhenEnabled() {
        ObjectNode chartNode = JsonNodeFactory.instance.objectNode();
        chartNode.put(Constants.JsonPaths.TRANSLATE_CODE, true);
        AggregateRequestDto request = new AggregateRequestDto();
        ArrayList<Object> codes = new ArrayList<>();
        codes.add("c1");
        codes.add("c2");
        request.setFilters(new HashMap<>(java.util.Map.of(Constants.MDMSKeys.TENANT_ID, codes)));
        when(mdmsApiMappings.valueOf("c1")).thenReturn("v1");
        when(mdmsApiMappings.valueOf("c2")).thenReturn("v2");

        clientService.preHandle(request, chartNode, mdmsApiMappings);

        @SuppressWarnings("unchecked")
        List<String> translated = (List<String>) request.getFilters().get(Constants.MDMSKeys.TENANT_ID);
        assertThat(translated).containsExactly("v1", "v2");
    }

    @Test
    void preHandle_skipsTranslationWhenDisabled() {
        ObjectNode chartNode = JsonNodeFactory.instance.objectNode();
        chartNode.put(Constants.JsonPaths.TRANSLATE_CODE, false);
        AggregateRequestDto request = new AggregateRequestDto();
        request.setFilters(new HashMap<>(java.util.Map.of(Constants.MDMSKeys.TENANT_ID, "raw")));

        clientService.preHandle(request, chartNode, mdmsApiMappings);

        assertThat(request.getFilters().get(Constants.MDMSKeys.TENANT_ID)).isEqualTo("raw");
        verify(mdmsApiMappings, never()).valueOf(org.mockito.ArgumentMatchers.anyString());
    }
}
