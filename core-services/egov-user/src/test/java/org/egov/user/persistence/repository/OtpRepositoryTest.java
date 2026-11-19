package org.egov.user.persistence.repository;

import org.egov.tracer.model.ServiceCallException;
import org.egov.user.Resources;
import org.egov.user.domain.model.OtpValidationRequest;
import org.egov.user.web.contract.Otp;
import org.egov.user.web.contract.OtpValidateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OtpRepositoryTest {

    private static final String OTP_HOST = "http://otp-host.com/";
    private static final String OTP_SEARCH = "otp/_search";
    private static final String OTP_VALIDATE = "otp/_validate";

    private OtpRepository otpRepository;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        otpRepository = new OtpRepository(OTP_HOST, OTP_SEARCH, OTP_VALIDATE, restTemplate);
    }

    @Test
    void isOtpValidationComplete_validatedOtp_returnsTrue() throws Exception {
        server.expect(once(), requestTo(OTP_HOST + OTP_SEARCH))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(new Resources().getFileContents("otpSearchValidatedResponse.json"),
                        MediaType.APPLICATION_JSON));

        boolean validated = otpRepository.isOtpValidationComplete(buildSearchRequest());

        server.verify();
        assertThat(validated).isTrue();
    }

    @Test
    void isOtpValidationComplete_pendingOtp_returnsFalse() throws Exception {
        server.expect(once(), requestTo(OTP_HOST + OTP_SEARCH))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(new Resources().getFileContents("otpSearchNonValidatedResponse.json"),
                        MediaType.APPLICATION_JSON));

        assertThat(otpRepository.isOtpValidationComplete(buildSearchRequest())).isFalse();
        server.verify();
    }

    @Test
    void validateOtp_successfulValidation_returnsTrue() throws Exception {
        server.expect(once(), requestTo(OTP_HOST + OTP_VALIDATE))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(new Resources().getFileContents("otpValidateResponse.json"),
                        MediaType.APPLICATION_JSON));

        assertThat(otpRepository.validateOtp(buildValidateRequest())).isTrue();
        server.verify();
    }

    @Test
    void validateOtp_failedValidation_returnsFalse() throws Exception {
        server.expect(once(), requestTo(OTP_HOST + OTP_VALIDATE))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(new Resources().getFileContents("otpNonValidateResponse.json"),
                        MediaType.APPLICATION_JSON));

        assertThat(otpRepository.validateOtp(buildValidateRequest())).isFalse();
        server.verify();
    }

    @Test
    void validateOtp_clientError_throwsServiceCallException() {
        server.expect(once(), requestTo(OTP_HOST + OTP_VALIDATE))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withBadRequest().body("{\"error\":\"invalid otp\"}"));

        assertThatThrownBy(() -> otpRepository.validateOtp(buildValidateRequest()))
                .isInstanceOf(ServiceCallException.class);
        server.verify();
    }

    @Test
    void validateOtp_emptyResponseBody_returnsFalse() {
        RestTemplate restTemplate = org.mockito.Mockito.mock(RestTemplate.class);
        OtpRepository repository = new OtpRepository(OTP_HOST, OTP_SEARCH, OTP_VALIDATE, restTemplate);
        org.mockito.Mockito.when(restTemplate.postForObject(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(
                        org.egov.user.persistence.dto.OtpResponse.class)))
                .thenReturn(null);

        assertThat(repository.validateOtp(buildValidateRequest())).isFalse();
    }

    private OtpValidationRequest buildSearchRequest() {
        return OtpValidationRequest.builder()
                .otpReference("2b936aae-c3b6-4c89-b3b3-a098cdcbb706")
                .mobileNumber("9988776655")
                .tenantId("tenantId")
                .build();
    }

    private OtpValidateRequest buildValidateRequest() {
        Otp otp = Otp.builder().otp("1234").tenantId("default").identity("9988776655").build();
        return OtpValidateRequest.builder().otp(otp).build();
    }
}
