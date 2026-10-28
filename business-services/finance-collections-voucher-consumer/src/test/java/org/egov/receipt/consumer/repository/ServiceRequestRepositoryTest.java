package org.egov.receipt.consumer.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.mdms.service.TokenService;
import org.egov.receipt.consumer.model.ProcessStatus;
import org.egov.receipt.consumer.model.RequestInfo;
import org.egov.receipt.consumer.model.RequestInfoWrapper;
import org.egov.receipt.custom.exception.VoucherCustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServiceRequestRepositoryTest {

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private TokenService tokenService;
    @Spy
    private ObjectMapper mapper = new ObjectMapper();
    @InjectMocks
    private ServiceRequestRepository repository;

    @Test
    void fetchResult_setsAdminTokenWhenMissing() throws Exception {
        RequestInfoWrapper wrapper = RequestInfoWrapper.builder().requestInfo(new RequestInfo()).build();
        Map<String, Object> body = Map.of("ok", true);
        when(tokenService.generateAdminToken("tenant")).thenReturn("admin-token");
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(body);

        Object result = repository.fetchResult(new StringBuilder("http://service/post"), wrapper, "tenant");

        assertThat(result).isEqualTo(body);
        assertThat(wrapper.getRequestInfo().getAuthToken()).isEqualTo("admin-token");
    }

    @Test
    void fetchResult_preservesExistingAuthToken() throws Exception {
        RequestInfo info = new RequestInfo();
        info.setAuthToken("existing");
        RequestInfoWrapper wrapper = RequestInfoWrapper.builder().requestInfo(info).build();
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(new HashMap<>());

        repository.fetchResult(new StringBuilder("http://service/post"), wrapper, "tenant");

        assertThat(wrapper.getRequestInfo().getAuthToken()).isEqualTo("existing");
        verify(tokenService, times(0)).generateAdminToken(anyString());
    }

    @Test
    void fetchResult_unauthorizedRetriesOnce() throws Exception {
        RequestInfoWrapper wrapper = RequestInfoWrapper.builder().requestInfo(new RequestInfo()).build();
        when(tokenService.generateAdminToken("tenant")).thenReturn("admin-token");
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED))
                .thenReturn(Map.of("retried", true));

        Object result = repository.fetchResult(new StringBuilder("http://service/post"), wrapper, "tenant");

        assertThat(result).isEqualTo(Map.of("retried", true));
        verify(restTemplate, times(2)).postForObject(anyString(), any(), eq(Map.class));
    }

    @Test
    void fetchResult_unauthorizedTwice_throwsVoucherCustomException() throws Exception {
        RequestInfoWrapper wrapper = RequestInfoWrapper.builder().requestInfo(new RequestInfo()).build();
        when(tokenService.generateAdminToken("tenant")).thenReturn("admin-token");
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> repository.fetchResult(new StringBuilder("http://service/post"), wrapper, "tenant"))
                .isInstanceOf(VoucherCustomException.class)
                .hasMessageContaining("SYSTEM auth token");
    }

    @Test
    void fetchResult_clientError_wrapsResponseBody() {
        RequestInfoWrapper wrapper = RequestInfoWrapper.builder().requestInfo(new RequestInfo()).build();
        when(tokenService.generateAdminToken("tenant")).thenReturn("admin-token");
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "bad", "body-error".getBytes(), null));

        assertThatThrownBy(() -> repository.fetchResult(new StringBuilder("http://service/post"), wrapper, "tenant"))
                .isInstanceOf(VoucherCustomException.class)
                .hasMessageContaining("body-error");
    }

    @Test
    void fetchResult_missingRequestInfo_throws() {
        assertThatThrownBy(() -> repository.fetchResult(new StringBuilder("http://x"), Map.of("noRequestInfo", true), "tenant"))
                .isInstanceOf(VoucherCustomException.class)
                .hasMessageContaining("Exception while fetching from searcher");
    }

    @Test
    void fetchResultGet_success() throws Exception {
        when(restTemplate.getForObject("http://ifsc", Map.class)).thenReturn(Map.of("BANK", "X"));

        assertThat(repository.fetchResultGet("http://ifsc")).isEqualTo(Map.of("BANK", "X"));
    }

    @Test
    void fetchResultGet_failure() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenThrow(new RuntimeException("down"));

        assertThatThrownBy(() -> repository.fetchResultGet("http://ifsc"))
                .isInstanceOf(VoucherCustomException.class)
                .hasMessageContaining("IFSC code is invalid");
    }
}
