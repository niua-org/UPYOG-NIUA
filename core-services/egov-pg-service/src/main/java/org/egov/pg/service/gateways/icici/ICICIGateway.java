package org.egov.pg.service.gateways.icici;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

//import org.egov.pg.models.Refund;
import org.egov.pg.models.Transaction;
import org.egov.pg.models.Transaction.TxnStatusEnum;
import org.egov.pg.service.Gateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * ICICI Gateway implementation
 */
@Service
@Slf4j
public class ICICIGateway implements Gateway {

    private static final String GATEWAY_NAME = "ICICI";
    private static final String CURRENCY_CODE = "356";

    private final String SECURE_SECRET;
    private final String MERCHANT_ID;
    private final String INITIATE_SALE_URL;
    private final String STATUS_CHECK_URL;
    private final String REDIRECT_URL;

    private final RestTemplate restTemplate;
    private ObjectMapper objectMapper;

    private final boolean ACTIVE;

    /**
     * Initialize by populating all required config parameters
     *
     * @param restTemplate rest template instance to be used to make REST calls
     * @param environment  containing all required config parameters
     * @param objectMapper
     */
    @Autowired
    public ICICIGateway(Environment environment, ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;

        ACTIVE = Boolean.valueOf(environment.getRequiredProperty("icici.active"));
        MERCHANT_ID = environment.getRequiredProperty("icici.merchant.id");
        SECURE_SECRET = environment.getRequiredProperty("icici.merchant.secret.key");
        INITIATE_SALE_URL = environment.getRequiredProperty("icici.gateway.url");
        STATUS_CHECK_URL = environment.getRequiredProperty("icici.gateway.url.status");
        REDIRECT_URL = environment.getRequiredProperty("icici.redirect.url");

    }

    @Override
    public URI generateRedirectURI(Transaction transaction) {

        try {

            log.info("ICICI request transaction: {}", transaction);
            Map<String, Object> request = buildInitiateSaleRequest(transaction);

            String secureHash = ICICIUtils.generateSecureHash(request, SECURE_SECRET);

            request.put("secureHash", secureHash);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            log.info("ICICI request : {}", request);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(INITIATE_SALE_URL, HttpMethod.POST, entity, Map.class);

            Map<String, Object> responseBody = response.getBody();

            log.info("ICICI initiate sale response : {}", responseBody);

            if (responseBody == null) {

                throw new RuntimeException("Empty response from ICICI");
            }

            String responseCode = (String) responseBody.get("responseCode");

            log.info("responseCode : {}", responseCode);

            if (!"R1000".equals(responseCode)) {

                throw new RuntimeException("ICICI payment initiation failed : " + responseBody);
            }

            String redirectURI = (String) responseBody.get("redirectURI");

            String tranCtx = (String) responseBody.get("tranCtx");

            return UriComponentsBuilder.fromHttpUrl(redirectURI).queryParam("tranCtx", tranCtx).build().toUri();

        } catch (Exception ex) {

            log.info("Error while generating ICICI redirect URI", ex);

            throw new RuntimeException("ICICI payment initiation failed", ex);
        }
    }

    @Override
    public Transaction fetchStatus(Transaction currentStatus, Map<String, String> params) {

        try {

            Map<String, Object> request = new HashMap<>();

            request.put("merchantId", MERCHANT_ID);
            request.put("merchantTxnNo", currentStatus.getTxnId());
            request.put("originalTxnNo", currentStatus.getTxnId());
            request.put("transactionType", "STATUS");

            String secureHash = ICICIUtils.generateSecureHash(request, SECURE_SECRET);

            request.put("secureHash", secureHash);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(STATUS_CHECK_URL, HttpMethod.POST, entity, Map.class);

            Map<String, Object> responseBody = response.getBody();

            log.info("ICICI status response : {}", responseBody);

            if (responseBody == null) {

                throw new RuntimeException("Empty response from ICICI");
            }

            currentStatus.setGatewayTxnId((String) responseBody.get("txnID"));

            currentStatus.setResponseJson(objectMapper.writeValueAsString(responseBody));

            String txnStatus = (String) responseBody.get("txnStatus");

            if ("SUC".equalsIgnoreCase(txnStatus)) {

                currentStatus.setTxnStatus(TxnStatusEnum.SUCCESS);

            } else if ("FAIL".equalsIgnoreCase(txnStatus)) {

                currentStatus.setTxnStatus(TxnStatusEnum.FAILURE);

            } else {

                currentStatus.setTxnStatus(TxnStatusEnum.PENDING);
            }

            return currentStatus;

        } catch (Exception ex) {

            log.info("Error while fetching ICICI payment status:{}", ex);

            throw new RuntimeException("ICICI payment status fetch failed", ex);
        }
    }

    @Override
    public boolean isActive() {

        return ACTIVE;
    }

    /**
     * ======================== GATEWAY NAME====================================
     */
    @Override
    public String gatewayName() {

        return GATEWAY_NAME;
    }

    /**
     * ================================================== CALLBACK TXN ID
     * KEY==================================================
     */
    @Override
    public String transactionIdKeyInResponse() {

        return "merchantTxnNo";
    }

    /**
     * ================================================== FORM
     * DATA==================================================
     */
    @Override
    public String generateRedirectFormData(Transaction transaction) {

        return null;
    }

    /**
     * ==================================== BUILD INITIATE SALE REQUEST
     * =============================================
     */
    private Map<String, Object> buildInitiateSaleRequest(Transaction transaction) {

        log.info("request transaction :{}", transaction);
        Map<String, Object> request = new HashMap<>();

        request.put("merchantId", MERCHANT_ID);
        request.put("merchantTxnNo", transaction.getTxnId());

        request.put("amount", new BigDecimal(transaction.getTxnAmount()).setScale(2, RoundingMode.HALF_UP).toString());

        request.put("currencyCode", CURRENCY_CODE);

        request.put("payType", "0");

        request.put("customerEmailID", transaction.getUser().getEmailId());

        request.put("transactionType", "SALE");

        request.put("returnURL", transaction.getCallbackUrl());

        request.put("txnDate", ICICIUtils.getCurrentTxnDate());

        request.put("customerMobileNo", transaction.getUser().getMobileNumber());

        request.put("customerName", transaction.getUser().getName());

        request.put("addlParam1", transaction.getProductInfo());
        request.put("addlParam2", transaction.getModule());

        log.info("request:{}", request);
        return request;
    }

//    @Override
//    public Refund initiateRefund(Refund refundTxn) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public Refund fetchRefundStatus(Refund refundRequest) {
//        // TODO Auto-generated method stub
//        return null;
//    }

}