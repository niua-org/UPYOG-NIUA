package com.example.gateway.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class UpyogMcpGatewaySupportTest {

    @Test
    void isMcpPath_prefix_returnsTrue() {
        assertThat(UpyogMcpGatewaySupport.isMcpPath("/upyog-mcp-server")).isTrue();
        assertThat(UpyogMcpGatewaySupport.isMcpPath("/upyog-mcp-server/mcp")).isTrue();
    }

    @Test
    void isMcpPath_unrelatedOrNull_returnsFalse() {
        assertThat(UpyogMcpGatewaySupport.isMcpPath("/user/_details")).isFalse();
        assertThat(UpyogMcpGatewaySupport.isMcpPath(null)).isFalse();
    }

    @Test
    void isMcpPath_prefixWithoutBoundary_returnsTrue() {
        assertThat(UpyogMcpGatewaySupport.isMcpPath("/upyog-mcp-server-other")).isTrue();
    }

    @Test
    void firstAuthToken_nullHeaders_returnsNull() {
        assertThat(UpyogMcpGatewaySupport.firstAuthToken(null)).isNull();
    }

    @Test
    void firstAuthToken_authTokenHeader_returnsTrimmedToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("auth-token", "  token-1  ");

        assertThat(UpyogMcpGatewaySupport.firstAuthToken(headers)).isEqualTo("token-1");
    }

    @Test
    void firstAuthToken_blankHeader_returnsNull() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Auth-Token", "   ");

        assertThat(UpyogMcpGatewaySupport.firstAuthToken(headers)).isNull();
    }

    @Test
    void firstAuthToken_missingHeader_returnsNull() {
        assertThat(UpyogMcpGatewaySupport.firstAuthToken(new HttpHeaders())).isNull();
    }
}
