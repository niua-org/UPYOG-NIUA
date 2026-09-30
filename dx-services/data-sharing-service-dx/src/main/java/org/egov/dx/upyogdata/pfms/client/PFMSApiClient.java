package org.egov.dx.upyogdata.pfms.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egov.dx.upyogdata.pfms.models.PFMSAuthResponse;
import org.egov.dx.upyogdata.pfms.models.PFMSTransaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * HTTP client responsible for all outbound calls to the PFMS external API.
 *
 * Two operations are handled:
 *   1. fetchAccessToken()  — authenticates with PFMS and returns a Bearer token
 *   2. pushTransaction()   — sends a single transaction record to PFMS using form-urlencoded body
 *
 * RestTemplate is used since PFMS API uses form-urlencoded (not JSON).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PFMSApiClient {

    private final RestTemplate restTemplate;

    @Value("${pfms.auth.url}")
    private String authUrl;

    @Value("${pfms.data.url}")
    private String dataUrl;

    @Value("${pfms.auth.username}")
    private String username;

    @Value("${pfms.auth.password}")
    private String password;

    @Value("${pfms.client.ip}")
    private String clientIp;

    /**
     * Authenticates with PFMS and retrieves a Bearer access token.
     *
     * Hits: POST /Auth/ValidLogin with JSON body {UserName, Password}
     * Returns the token string on success, throws RuntimeException on failure.
     *
     * @return Bearer token string to be used in subsequent API calls
     */
    public String fetchAccessToken() {
        log.info("Fetching access token from PFMS auth API");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("UserName", username, "Password", password);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<PFMSAuthResponse> response = restTemplate.postForEntity(
                    authUrl, request, PFMSAuthResponse.class);

            PFMSAuthResponse authResponse = response.getBody();
            if (authResponse == null || authResponse.getAccessToken() == null) {
                throw new RuntimeException("Pfms Auth response is null or token missing");
            }

            log.info("Pfms Auth Access token fetched successfully");
            return authResponse.getAccessToken();

        } catch (Exception e) {
            log.error("Pfms Auth Failed to fetch access token from PFMS: {}", e.getMessage());
            throw new RuntimeException("PFMS authentication failed", e);
        }
    }

    /**
     * Pushes a single PFMS transaction to the PFMS data API.
     *
     * Hits: POST /ULBAPI/GetTransactionalDataFetchedFromULBSystem
     * Content-Type: application/x-www-form-urlencoded
     * Authorization: Bearer {token}
     *
     * Each field from PFMSTransaction is mapped to the corresponding
     * PFMS API field name (PascalCase as expected by PFMS).
     *
     * @param transaction the transaction to push
     * @param accessToken Bearer token obtained from fetchAccessToken()
     * @return raw response body string from PFMS (saved as-is in DB)
     */
    public String pushTransaction(PFMSTransaction transaction, String accessToken) {
        log.info("Pushing transaction to Pfms | voucherNumber={} voucherDate={}",
                transaction.getVoucherNumber(), transaction.getVoucherDate());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBearerAuth(accessToken);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("ULBCodeOrPFMSAgencyCode",        transaction.getUlbCodeOrPFMSAgencyCode());
        formData.add("VoucherNumber",                   transaction.getVoucherNumber());
        formData.add("VoucherDate",                     transaction.getVoucherDate().format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd")));
        formData.add("VoucherType",                     transaction.getVoucherType());
        formData.add("FinancialYear",                   transaction.getFinancialYear());
        formData.add("AccountHeadCode",                 transaction.getAccountHeadCode());
        formData.add("FunctionCode",                    transaction.getFunctionCode());
        formData.add("SchemeCode",                      transaction.getSchemeCode());
        formData.add("DebitAmount",                     nullSafe(transaction.getDebitAmount()));
        formData.add("CreditAmount",                    nullSafe(transaction.getCreditAmount()));
        formData.add("NarrationOrDescription",          transaction.getNarrationOrDescription());
        formData.add("VoucherStatus",                   transaction.getVoucherStatus());
        formData.add("ULBBankAccountNumber",            transaction.getUlbBankAccountNumber());
        formData.add("ULBIFSCCode",                     transaction.getUlbIFSCCode());
        formData.add("InstrumentReference",             transaction.getInstrumentReference());
        formData.add("ModeOfTransaction",               transaction.getModeOfTransaction());
        formData.add("BeneficiaryOrPayeeName",          transaction.getBeneficiaryOrPayeeName());
        formData.add("BeneficiaryAccountNumber",        transaction.getBeneficiaryAccountNumber());
        formData.add("BeneficiaryIFSCCode",             transaction.getBeneficiaryIFSCCode());
        formData.add("BeneficiaryType",                 transaction.getBeneficiaryType());
        formData.add("ChallanNumber",                   transaction.getChallanNumber());
        formData.add("FromAccount",                     transaction.getFromAccount());
        formData.add("ToAccount",                       transaction.getToAccount());
        formData.add("ContraNature",                    transaction.getContraNature());
        formData.add("TransferInstructionReference",    transaction.getTransferInstructionReference());
        formData.add("ReferenceVoucherNumber",          transaction.getReferenceVoucherNumber());
        formData.add("AdjustmentType",                  transaction.getAdjustmentType());
        formData.add("RelatedAssetOrLiabilityCode",     transaction.getRelatedAssetOrLiabilityCode());
        formData.add("DebtorOrCreditorReference",       transaction.getDebtorOrCreditorReference());
        formData.add("BasisOfAdjustment",               transaction.getBasisOfAdjustment());
        formData.add("PeriodCovered",                   transaction.getPeriodCovered());
        formData.add("ClientIP",                        clientIp);
        log.info("Form data being sent to PFMS: {}", formData);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(dataUrl, request, String.class);
            log.info("Pfms Response received | voucherNumber={} httpStatus={} body={}",
                    transaction.getVoucherNumber(), response.getStatusCode(), response.getBody());
            return response.getBody();

        } catch (Exception e) {
            log.error("Pfms Failed to push transaction | voucherNumber={} error={}",
                    transaction.getVoucherNumber(), e.getMessage());
            throw new RuntimeException("PFMS data push failed for voucher: " + transaction.getVoucherNumber(), e);
        }
    }

    private String nullSafe(Object val) {
        return val == null ? "" : val.toString();
    }
}
