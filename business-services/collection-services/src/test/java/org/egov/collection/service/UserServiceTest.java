package org.egov.collection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;

import org.egov.collection.config.ApplicationProperties;
import org.egov.collection.model.Payment;
import org.egov.collection.model.PaymentRequest;
import org.egov.collection.web.contract.Bill;
import org.egov.collection.web.contract.UserResponse;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ApplicationProperties properties;

    @Test
    void getUser_returnsUuidWhenUserExists() {
        when(properties.getUserHost()).thenReturn("http://user/");
        when(properties.getUserSearchEnpoint()).thenReturn("search");
        UserResponse response = new UserResponse();
        response.setReceiptCreators(List.of(User.builder().uuid("uuid-1").build()));
        when(restTemplate.postForObject(eq("http://user/search"), any(), eq(UserResponse.class))).thenReturn(response);

        assertThat(userService.getUser(new RequestInfo(), "9999999999", "pb.amritsar"))
                .containsEntry("id", "uuid-1");
    }

    @Test
    void getUser_returnsEmptyMapWhenRestCallFails() {
        when(properties.getUserHost()).thenReturn("http://user/");
        when(properties.getUserSearchEnpoint()).thenReturn("search");
        when(restTemplate.postForObject(eq("http://user/search"), any(), eq(UserResponse.class)))
                .thenThrow(new RuntimeException("down"));

        assertThat(userService.getUser(new RequestInfo(), "9999999999", "pb.amritsar")).isEmpty();
    }

    @Test
    void createUserFromBill_returnsCreatedUuid() {
        when(properties.getUserHost()).thenReturn("http://user/");
        when(properties.getUserCreateEnpoint()).thenReturn("create");
        UserResponse response = new UserResponse();
        response.setReceiptCreators(List.of(User.builder().uuid("new-user").build()));
        when(restTemplate.postForObject(eq("http://user/create"), any(), eq(UserResponse.class))).thenReturn(response);

        Bill bill = new Bill();
        bill.setTenantId("pb.amritsar");
        bill.setPaidBy("Citizen");
        bill.setMobileNumber("8888888888");
        bill.setPayerAddress("Address");

        assertThat(userService.createUser(new RequestInfo(), bill)).isEqualTo("new-user");
    }

    @Test
    void createUserFromPaymentRequest_returnsUuid() {
        when(properties.getUserHost()).thenReturn("http://user/");
        when(properties.getUserCreateEnpoint()).thenReturn("create");
        UserResponse response = new UserResponse();
        response.setReceiptCreators(List.of(User.builder().uuid("pay-user").build()));
        when(restTemplate.postForObject(eq("http://user/create"), any(), eq(UserResponse.class))).thenReturn(response);

        Payment payment = new Payment();
        payment.setTenantId("pb.amritsar");
        payment.setPaidBy("Citizen");
        payment.setMobileNumber("7777777777");
        payment.setPayerAddress("Address");

        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setRequestInfo(new RequestInfo());
        paymentRequest.setPayment(payment);

        assertThat(userService.createUser(paymentRequest)).isEqualTo("pay-user");
    }
}
