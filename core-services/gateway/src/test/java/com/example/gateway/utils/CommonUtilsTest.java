package com.example.gateway.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommonUtilsTest {

    private CommonUtils commonUtils;

    @Mock
    private ServerWebExchange exchange;

    @BeforeEach
    void setUp() {
        commonUtils = new CommonUtils(new ObjectMapper());
    }

    @Test
    void isRequestBodyCompatible_postJson_returnsTrue() {
        ServerHttpRequest request = requestWithoutQuery(HttpMethod.POST, "application/json");

        assertThat(CommonUtils.isRequestBodyCompatible(request)).isTrue();
    }

    @Test
    void isRequestBodyCompatible_putJsonWithCharset_returnsTrue() {
        ServerHttpRequest request = requestWithoutQuery(HttpMethod.PUT, "application/json;charset=UTF-8");

        assertThat(CommonUtils.isRequestBodyCompatible(request)).isTrue();
    }

    @Test
    void isRequestBodyCompatible_patchFormUrlEncoded_returnsTrue() {
        ServerHttpRequest request = requestWithoutQuery(HttpMethod.PATCH, "application/x-www-form-urlencoded");

        assertThat(CommonUtils.isRequestBodyCompatible(request)).isTrue();
    }

    @Test
    void isRequestBodyCompatible_getJson_returnsFalse() {
        ServerHttpRequest request = requestWithoutQuery(HttpMethod.GET, "application/json");

        assertThat(CommonUtils.isRequestBodyCompatible(request)).isFalse();
    }

    @Test
    void isRequestBodyCompatible_postMultipart_returnsFalse() {
        ServerHttpRequest request = requestWithoutQuery(HttpMethod.POST, "multipart/form-data");

        assertThat(CommonUtils.isRequestBodyCompatible(request)).isFalse();
    }

    @Test
    void isRequestBodyCompatible_postPlainText_returnsFalse() {
        ServerHttpRequest request = requestWithoutQuery(HttpMethod.POST, "text/plain");

        assertThat(CommonUtils.isRequestBodyCompatible(request)).isFalse();
    }

    @Test
    void isRequestBodyCompatible_postWithoutContentType_returnsFalse() {
        ServerHttpRequest request = requestWithoutQuery(HttpMethod.POST, null);

        assertThat(CommonUtils.isRequestBodyCompatible(request)).isFalse();
    }

    @Test
    void isRequestBodyCompatible_nullMethod_throwsNullPointerException() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getMethod()).thenReturn(null);

        assertThatThrownBy(() -> CommonUtils.isRequestBodyCompatible(request))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void getRequestContentType_missingHeader_returnsEmptyString() {
        ServerHttpRequest request = requestWithoutQuery(HttpMethod.POST, null);

        assertThat(CommonUtils.getRequestContentType(request)).isEmpty();
    }

    @Test
    void getRequestContentType_emptyHeaderList_returnsEmptyString() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        HttpHeaders headers = mock(HttpHeaders.class);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.get(HttpHeaders.CONTENT_TYPE)).thenReturn(List.of());

        assertThat(CommonUtils.getRequestContentType(request)).isEmpty();
    }

    @Test
    void getRequestContentType_mixedCaseHeader_returnsLowerCase() {
        ServerHttpRequest request = requestWithoutQuery(HttpMethod.POST, "Application/JSON");

        assertThat(CommonUtils.getRequestContentType(request)).isEqualTo("application/json");
    }

    @Test
    void getLowLevelTenantIdFromSet_emptySet_returnsNull() {
        assertThat(commonUtils.getLowLevelTenantIdFromSet(Collections.emptySet())).isNull();
    }

    @Test
    void getLowLevelTenantIdFromSet_nullSet_throwsNullPointerException() {
        assertThatThrownBy(() -> commonUtils.getLowLevelTenantIdFromSet(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void getLowLevelTenantIdFromSet_singleTenant_returnsThatTenant() {
        assertThat(commonUtils.getLowLevelTenantIdFromSet(Set.of("pg"))).isEqualTo("pg");
    }

    @Test
    void getLowLevelTenantIdFromSet_deeperTenant_returnsDeepest() {
        Set<String> tenants = new LinkedHashSet<>();
        tenants.add("pg");
        tenants.add("pg.cityb");
        tenants.add("pg.citya.ward1");

        assertThat(commonUtils.getLowLevelTenantIdFromSet(tenants)).isEqualTo("pg.citya.ward1");
    }

    @Test
    void getLowLevelTenantIdFromSet_equalDepth_returnsLastSeen() {
        Set<String> cityAThenCityB = new LinkedHashSet<>();
        cityAThenCityB.add("pg.citya");
        cityAThenCityB.add("pg.cityb");

        Set<String> cityBThenCityA = new LinkedHashSet<>();
        cityBThenCityA.add("pg.cityb");
        cityBThenCityA.add("pg.citya");

        assertThat(commonUtils.getLowLevelTenantIdFromSet(cityAThenCityB)).isEqualTo("pg.cityb");
        assertThat(commonUtils.getLowLevelTenantIdFromSet(cityBThenCityA)).isEqualTo("pg.citya");
    }

    @Test
    void getTenantIdsFromRequest_jsonTenantId_returnsPayloadTenant() {
        Map<String, Object> body = new HashMap<>();
        body.put("RequestInfo", Map.of("userInfo", Map.of("tenantId", "from-user")));
        body.put("Property", Map.of("tenantId", "pg.citya"));

        Set<String> tenantIds = commonUtils.getTenantIdsFromRequest(jsonPost(null), body);

        assertThat(tenantIds).containsExactly("pg.citya");
    }

    @Test
    void getTenantIdsFromRequest_tenantIdInsideRequestInfo_isIgnored() {
        Map<String, Object> body = new HashMap<>();
        body.put("RequestInfo", Map.of(
                "tenantId", "from-pascal",
                "userInfo", Map.of("tenantId", "from-user")));
        body.put("tenantId", "pg.cityb");

        Set<String> tenantIds = commonUtils.getTenantIdsFromRequest(jsonPost(query("pg.from-query")), body);

        assertThat(tenantIds).containsExactly("pg.cityb");
    }

    @Test
    void getTenantIdsFromRequest_camelCaseRequestInfoTenantId_isIgnored() {
        Map<String, Object> body = new HashMap<>();
        body.put("requestInfo", Map.of("tenantId", "from-camel", "userInfo", Map.of("tenantId", "from-user")));
        body.put("tenantId", "pg.cityc");

        Set<String> tenantIds = commonUtils.getTenantIdsFromRequest(jsonPost(null), body);

        assertThat(tenantIds).containsExactly("pg.cityc");
    }

    @Test
    void getTenantIdsFromRequest_bothRequestInfoCasings_keepsCamelCaseTenantId() {
        Map<String, Object> body = new HashMap<>();
        body.put("RequestInfo", Map.of("tenantId", "from-pascal"));
        body.put("requestInfo", Map.of("tenantId", "from-camel"));

        Set<String> tenantIds = commonUtils.getTenantIdsFromRequest(jsonPost(null), body);

        assertThat(tenantIds).containsExactly("from-camel");
    }

    @Test
    void getTenantIdsFromRequest_missingBodyTenant_usesQueryParam() {
        ServerHttpRequest request = jsonPost(query("pg.citya"));

        Set<String> tenantIds = commonUtils.getTenantIdsFromRequest(request, new HashMap<>());

        assertThat(tenantIds).containsExactly("pg.citya");
    }

    @Test
    void getTenantIdsFromRequest_missingBodyAndQuery_fallsBackToPg() {
        Set<String> tenantIds = commonUtils.getTenantIdsFromRequest(jsonPost(null), new HashMap<>());

        assertThat(tenantIds).containsExactly("pg");
    }

    @Test
    void getTenantIdsFromRequest_getWithBodyTenantAndNoQuery_fallsBackToPg() {
        Map<String, Object> body = new HashMap<>();
        body.put("tenantId", "pg.citya");

        Set<String> tenantIds = commonUtils.getTenantIdsFromRequest(request(HttpMethod.GET, null, query()), body);

        assertThat(tenantIds).containsExactly("pg");
    }

    @Test
    void getTenantIdsFromRequest_commaSeparatedQueryTenants_returnsEachTenant() {
        ServerHttpRequest request = request(HttpMethod.GET, null, query("pg.citya,pg.cityb"));

        Set<String> tenantIds = commonUtils.getTenantIdsFromRequest(request, new HashMap<>());

        assertThat(tenantIds).containsExactlyInAnyOrder("pg.citya", "pg.cityb");
    }

    @Test
    void getTenantIdsFromRequest_commaSeparatedQueryWithSpace_keepsLeadingSpace() {
        ServerHttpRequest request = request(HttpMethod.GET, null, query("pg.citya, pg.cityb"));

        Set<String> tenantIds = commonUtils.getTenantIdsFromRequest(request, new HashMap<>());

        assertThat(tenantIds).containsExactlyInAnyOrder("pg.citya", " pg.cityb");
    }

    @Test
    void getTenantIdsFromRequest_nullAndNullStringTenants_areIgnored() {
        Map<String, Object> body = new HashMap<>();
        body.put("tenantId", Arrays.asList("null", "NULL", "pg.citya"));

        Set<String> tenantIds = commonUtils.getTenantIdsFromRequest(jsonPost(query("pg.from-query")), body);

        assertThat(tenantIds).containsExactly("pg.citya");
    }

    @Test
    void getTenantIdsFromRequest_onlyNullStringTenant_doesNotFallBack() {
        Map<String, Object> body = new HashMap<>();
        body.put("tenantId", "null");

        Set<String> tenantIds = commonUtils.getTenantIdsFromRequest(jsonPost(query("pg.citya")), body);

        assertThat(tenantIds).isEmpty();
    }

    @Test
    void getTenantIdsFromRequest_jsonNullTenantValue_fallsBackToQueryParam() {
        Map<String, Object> body = new HashMap<>();
        body.put("tenantId", null);

        Set<String> tenantIds = commonUtils.getTenantIdsFromRequest(jsonPost(query("pg.citya")), body);

        assertThat(tenantIds).containsExactly("pg.citya");
    }

    @Test
    void getTenantIdsFromRequest_queryNullString_isKept() {
        ServerHttpRequest request = request(HttpMethod.GET, null, query("null"));

        Set<String> tenantIds = commonUtils.getTenantIdsFromRequest(request, new HashMap<>());

        assertThat(tenantIds).containsExactly("null");
    }

    @Test
    void getTenantIdsFromRequest_numericTenantId_fallsBackToQueryParam() {
        Map<String, Object> body = new HashMap<>();
        body.put("tenantId", 12);

        Set<String> tenantIds = commonUtils.getTenantIdsFromRequest(jsonPost(query("pg.citya")), body);

        assertThat(tenantIds).containsExactly("pg.citya");
    }

    @Test
    void getTenantIdsFromRequest_jsonTenantPresent_ignoresQueryParam() {
        Map<String, Object> body = new HashMap<>();
        body.put("tenantId", "pg.cityb");

        Set<String> tenantIds = commonUtils.getTenantIdsFromRequest(jsonPost(query("pg.from-query")), body);

        assertThat(tenantIds).containsExactly("pg.cityb");
    }

    @Test
    void getTenantIdsFromRequest_nullBody_throwsParseFailed() {
        assertThatThrownBy(() -> commonUtils.getTenantIdsFromRequest(jsonPost(null), null))
                .isInstanceOfSatisfying(CustomException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("401 UNAUTHORIZED");
                    assertThat(exception.getMessage()).isEqualTo("Failed to parse request at API gateway");
                });
    }

    @Test
    void getTenantIdsFromRequest_formUrlEncodedBodyTenant_returnsPayloadTenant() {
        Map<String, Object> body = new HashMap<>();
        body.put("tenantId", "pg.form");
        ServerHttpRequest request = request(HttpMethod.POST, "application/x-www-form-urlencoded", query());

        Set<String> tenantIds = commonUtils.getTenantIdsFromRequest(request, body);

        assertThat(tenantIds).containsExactly("pg.form");
    }

    @Test
    void validateRequestAndSetRequestTenantId_jsonTenant_delegatesToRequest() {
        Map<String, Object> body = new HashMap<>();
        body.put("tenantId", "pg.citya");
        ServerHttpRequest request = jsonPost(query());
        when(exchange.getRequest()).thenReturn(request);

        Set<String> tenantIds = commonUtils.validateRequestAndSetRequestTenantId(exchange, body);

        assertThat(tenantIds).containsExactly("pg.citya");
    }

    @Test
    void setTenantIdsFromQueryParams_emptyParams_addsPg() {
        Set<String> tenantIds = new LinkedHashSet<>();

        commonUtils.setTenantIdsFromQueryParams(query(), tenantIds);

        assertThat(tenantIds).containsExactly("pg");
    }

    @Test
    void setTenantIdsFromQueryParams_multipleQueryValues_usesFirstOnly() {
        LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("tenantId", "pg.first");
        params.add("tenantId", "pg.second");
        Set<String> tenantIds = new LinkedHashSet<>();

        commonUtils.setTenantIdsFromQueryParams(params, tenantIds);

        assertThat(tenantIds).containsExactly("pg.first");
    }

    private static ServerHttpRequest jsonPost(MultiValueMap<String, String> queryParams) {
        if (queryParams == null) {
            return requestWithoutQuery(HttpMethod.POST, "application/json");
        }
        return request(HttpMethod.POST, "application/json", queryParams);
    }

    private static ServerHttpRequest request(HttpMethod method, String contentType, MultiValueMap<String, String> queryParams) {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        lenient().when(request.getMethod()).thenReturn(method);
        HttpHeaders headers = new HttpHeaders();
        if (contentType != null) {
            headers.set(HttpHeaders.CONTENT_TYPE, contentType);
        }
        when(request.getHeaders()).thenReturn(headers);
        if (queryParams != null) {
            lenient().when(request.getQueryParams()).thenReturn(queryParams);
        }
        return request;
    }

    private static ServerHttpRequest requestWithoutQuery(HttpMethod method, String contentType) {
        return request(method, contentType, null);
    }

    private static LinkedMultiValueMap<String, String> query(String... tenantIds) {
        LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        if (tenantIds.length > 0) {
            params.add("tenantId", tenantIds[0]);
        }
        return params;
    }
}
