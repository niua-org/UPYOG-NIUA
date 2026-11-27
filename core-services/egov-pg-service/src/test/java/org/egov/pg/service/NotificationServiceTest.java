package org.egov.pg.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.pg.config.AppProperties;
import org.egov.pg.models.CollectionPayment;
import org.egov.pg.models.CollectionPaymentRequest;
import org.egov.pg.models.Refund;
import org.egov.pg.models.RefundRequest;
import org.egov.pg.models.Transaction;
import org.egov.pg.utils.NotificationUtil;
import org.egov.pg.web.models.SMSRequest;
import org.egov.pg.web.models.TransactionRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NotificationServiceTest {

    @Mock
    private AppProperties appProperties;

    @Mock
    private NotificationUtil notificationUtil;

    @InjectMocks
    private NotificationService notificationService;

    private RequestInfo systemRequestInfo;
    private RequestInfo citizenRequestInfo;

    @Before
    public void setUp() {
        systemRequestInfo = RequestInfo.builder()
                .userInfo(User.builder().type("SYSTEM").mobileNumber("9999999999").build())
                .build();
        citizenRequestInfo = RequestInfo.builder()
                .userInfo(User.builder().type("CITIZEN").mobileNumber("8888888888").build())
                .build();
        when(appProperties.getIsSMSEnable()).thenReturn(true);
        when(appProperties.getNotificationHost()).thenReturn("https://host/");
        when(appProperties.getApplicationPayLink()).thenReturn("/pay/$tenantId/$businessService/$consumerCode");
    }

    @Test
    public void smsNotificationSendsWhenSystemUser() {
        Transaction transaction = Transaction.builder()
                .tenantId("pb")
                .txnAmount("100")
                .consumerCode("PT-1")
                .txnId("TXN-1")
                .txnStatus(Transaction.TxnStatusEnum.SUCCESS)
                .txnStatusMsg("ok")
                .user(org.egov.pg.web.models.User.builder().mobileNumber("7777777777").build())
                .build();
        TransactionRequest request = new TransactionRequest(systemRequestInfo, transaction);
        when(notificationUtil.getLocalizationMessages(anyString(), any(), anyString())).thenReturn("{}");
        when(notificationUtil.getCustomizedMsg(anyString(), anyString())).thenReturn("Paid {amount} for {consumerCode}");

        notificationService.smsNotification(request, "topic");

        verify(notificationUtil).sendSMS(any());
    }

    @Test
    public void smsNotificationSkippedForCitizen() {
        Transaction transaction = Transaction.builder()
                .txnStatus(Transaction.TxnStatusEnum.SUCCESS)
                .user(org.egov.pg.web.models.User.builder().mobileNumber("7777777777").build())
                .build();
        TransactionRequest request = new TransactionRequest(citizenRequestInfo, transaction);

        notificationService.smsNotification(request, "topic");

        verify(notificationUtil, never()).sendSMS(any());
    }

    @Test
    public void smsNotificationSkippedWhenSmsDisabled() {
        when(appProperties.getIsSMSEnable()).thenReturn(false);
        TransactionRequest request = new TransactionRequest(systemRequestInfo,
                Transaction.builder().txnStatus(Transaction.TxnStatusEnum.SUCCESS).build());

        notificationService.smsNotification(request, "topic");

        verify(notificationUtil, never()).sendSMS(any());
    }

    @Test
    public void getSmsRequestReplacesPlaceholdersIncludingPaymentLink() {
        Transaction transaction = Transaction.builder()
                .tenantId("pb")
                .txnAmount("150")
                .consumerCode("PT-99")
                .txnId("TXN-99")
                .txnStatus(Transaction.TxnStatusEnum.FAILURE)
                .txnStatusMsg("declined")
                .user(org.egov.pg.web.models.User.builder().mobileNumber("6666666666").build())
                .build();
        TransactionRequest request = new TransactionRequest(systemRequestInfo, transaction);
        when(notificationUtil.getLocalizationMessages(eq("pb"), any(), anyString())).thenReturn("{}");
        when(notificationUtil.getCustomizedMsg(anyString(), anyString()))
                .thenReturn("Pay {amount} {consumerCode} {txnId} {Payment failure reason} {payment link}");
        when(notificationUtil.getBusinessService(request)).thenReturn("PT");
        when(notificationUtil.getShortnerURL(anyString())).thenReturn("https://short/pay");

        List<SMSRequest> smsRequests = notificationService.getSmsRequest(request, "topic");

        assertEquals(1, smsRequests.size());
        String message = smsRequests.get(0).getMessage();
        assertTrue(message.contains("150"));
        assertTrue(message.contains("PT-99"));
        assertTrue(message.contains("TXN-99"));
        assertTrue(message.contains("declined"));
        assertTrue(message.contains("https://short/pay"));
    }

    @Test
    public void getSmsRequestReturnsEmptyWhenNoTemplate() {
        TransactionRequest request = new TransactionRequest(systemRequestInfo,
                Transaction.builder()
                        .txnStatus(Transaction.TxnStatusEnum.SUCCESS)
                        .user(org.egov.pg.web.models.User.builder().mobileNumber("6666666666").build())
                        .build());
        assertTrue(notificationService.getSmsRequest(request, "topic").isEmpty());
    }

    @Test
    public void refundInitiateSmsNotificationSendsForSystemUser() {
        Refund refund = Refund.builder()
                .tenantId("pb")
                .refundId("RF-1")
                .refundAmount(new BigDecimal("50"))
                .status(Refund.RefundStatusEnum.INITIATED)
                .gatewayStatusMsg("pending")
                .build();
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setRequestInfo(systemRequestInfo);
        refundRequest.setRefund(refund);
        when(notificationUtil.getLocalizationMessages(anyString(), any(), anyString())).thenReturn("{}");
        when(notificationUtil.getCustomizedMsgByNotificationType(anyString(), anyString(), anyString()))
                .thenReturn("Refund {amount} {txnId} {Refund failure reason}");

        notificationService.refundInitiateSmsNotification(refundRequest, "refund-topic");

        verify(notificationUtil).sendSMS(any());
    }

    @Test
    public void getRefundInitiateSmsRequestSkipsBlankMobile() {
        Refund refund = Refund.builder()
                .tenantId("pb")
                .refundId("RF-2")
                .refundAmount(BigDecimal.TEN)
                .status(Refund.RefundStatusEnum.INITIATED)
                .build();
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setRequestInfo(RequestInfo.builder()
                .userInfo(User.builder().type("SYSTEM").mobileNumber("").build())
                .build());
        refundRequest.setRefund(refund);
        when(notificationUtil.getLocalizationMessages(anyString(), any(), anyString())).thenReturn("{}");
        when(notificationUtil.getCustomizedMsgByNotificationType(anyString(), anyString(), anyString()))
                .thenReturn("Refund {amount}");

        assertTrue(notificationService.getRefundInitiateSmsRequest(refundRequest, "t").isEmpty());
    }

    @Test
    public void refundSuccessSmsNotificationSendsForSystemUser() {
        CollectionPayment payment = CollectionPayment.builder()
                .tenantId("pb")
                .totalAmountPaid(new BigDecimal("75"))
                .transactionNumber("TXN-75")
                .mobileNumber("5555555555")
                .build();
        CollectionPaymentRequest paymentRequest = CollectionPaymentRequest.builder()
                .requestInfo(systemRequestInfo)
                .payment(payment)
                .build();
        when(notificationUtil.getLocalizationMessages(anyString(), any(), anyString())).thenReturn("{}");
        when(notificationUtil.getCustomizedMsgByNotificationType(anyString(), eq("SUCCESS"), anyString()))
                .thenReturn("Refunded {amount} for {txnId}");

        notificationService.refundSuccessSmsNotification(paymentRequest, "success-topic");

        verify(notificationUtil).sendSMS(any());
    }

    @Test
    public void getRefundSuccessSmsRequestBuildsMessage() {
        CollectionPayment payment = CollectionPayment.builder()
                .tenantId("pb")
                .totalAmountPaid(new BigDecimal("80"))
                .transactionNumber("TXN-80")
                .mobileNumber("4444444444")
                .build();
        CollectionPaymentRequest paymentRequest = CollectionPaymentRequest.builder()
                .requestInfo(systemRequestInfo)
                .payment(payment)
                .build();
        when(notificationUtil.getLocalizationMessages(anyString(), any(), anyString())).thenReturn("{}");
        when(notificationUtil.getCustomizedMsgByNotificationType(anyString(), eq("SUCCESS"), anyString()))
                .thenReturn("Done {amount} {txnId}");

        List<SMSRequest> requests = notificationService.getRefundSuccessSmsRequest(paymentRequest, "t");

        assertEquals(1, requests.size());
        assertTrue(requests.get(0).getMessage().contains("80"));
        assertTrue(requests.get(0).getMessage().contains("TXN-80"));
    }
}
