package org.egov.user.domain.service.utils;

import org.egov.common.contract.request.RequestInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalizationUtilTest {

    @Mock
    private RestTemplate restTemplate;

    private LocalizationUtil localizationUtil;

    @BeforeEach
    void setUp() {
        localizationUtil = new LocalizationUtil();
        ReflectionTestUtils.setField(localizationUtil, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(localizationUtil, "localizationServiceHost", "http://localization");
        ReflectionTestUtils.setField(localizationUtil, "localizationServiceSearchPath", "/localization/v1/_search");
        ReflectionTestUtils.setField(localizationUtil, "tenantId", "pg");
        ReflectionTestUtils.setField(localizationUtil, "module", "egov-user");
        ReflectionTestUtils.setField(localizationUtil, "defaultLocale", "en_IN");
    }

    @Test
    void getLocalizedMessage_resolvesCodeFromResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("messages", List.of(Map.of("code", "USER_NOT_FOUND", "message", "User missing")));
        when(restTemplate.postForObject(any(String.class), any(RequestInfo.class), eq(Map.class)))
                .thenReturn(response);

        String message = localizationUtil.getLocalizedMessage("USER_NOT_FOUND", null, RequestInfo.builder().build());

        assertThat(message).isEqualTo("User missing");
    }

    @Test
    void getUri_buildsSearchUrl() {
        assertThat(localizationUtil.getUri("en_IN"))
                .isEqualTo("http://localization/localization/v1/_search?locale=en_IN&tenantId=pg&module=egov-user");
    }
}
