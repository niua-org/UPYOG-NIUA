package org.egov.inbox.util;

import org.egov.inbox.config.InboxConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElasticSearchUtilTest {

    @Mock
    private InboxConfiguration config;

    @InjectMocks
    private ElasticSearchUtil elasticSearchUtil;

    @BeforeEach
    void stubCredentials() {
        when(config.getUserName()).thenReturn("es-user");
        when(config.getPassword()).thenReturn("es-pass");
    }

    @Test
    void getESEncodedCredentials_returnsBasicAuthHeaderValue() {
        String expected = "Basic "
                + Base64.getEncoder().encodeToString("es-user:es-pass".getBytes(StandardCharsets.UTF_8));

        assertThat(elasticSearchUtil.getESEncodedCredentials()).isEqualTo(expected);
    }

    @Test
    void getHttpHeaders_setsAuthorizationAndAcceptJson() {
        HttpHeaders headers = elasticSearchUtil.getHttpHeaders();

        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo(elasticSearchUtil.getESEncodedCredentials());
        assertThat(headers.getAccept()).contains(org.springframework.http.MediaType.APPLICATION_JSON);
    }
}
