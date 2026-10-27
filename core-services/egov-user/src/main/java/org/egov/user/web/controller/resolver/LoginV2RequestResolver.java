package org.egov.user.web.controller.resolver;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.user.web.contract.multiauth.AuthType;
import org.egov.user.web.contract.multiauth.LoginV2Request;
import org.springframework.stereotype.Component;

/**
 * Resolver component responsible for parsing, defaulting, and enriching incoming {@link LoginV2Request} payloads.
 *
 * <p>Adheres strictly to the Single Responsibility Principle (SRP):
 * Extracts HTTP headers, client IP addresses, and form/query fallback parameters away from the REST controller.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class LoginV2RequestResolver {

    /**
     * Resolves the effective {@link LoginV2Request} from request body and servlet request parameters.
     *
     * @param requestBody optional JSON body binding
     * @param servletRequest underlying servlet request for query/form parameter extraction
     * @return fully enriched and normalized {@link LoginV2Request}
     */
    public LoginV2Request resolve(LoginV2Request requestBody, HttpServletRequest servletRequest) {
        String clientIp = resolveClientIp(servletRequest);
        log.debug("[LOGIN-V2-RESOLVER] Resolving loginv2 request from client IP: {}", clientIp);

        LoginV2Request target = (requestBody != null) ? requestBody : new LoginV2Request();

        // 1. Fallback parameter binding from query / form-data
        if (servletRequest != null) {
            enrichFromParameters(target, servletRequest);
        }

        // 2. Default RequestInfo if absent
        if (target.getRequestInfo() == null) {
            log.debug("[LOGIN-V2-RESOLVER] RequestInfo missing in payload. Generating synthetic RequestInfo.");
            target.setRequestInfo(RequestInfo.builder()
                    .apiId("egov-user-loginv2")
                    .ver("2.0")
                    .ts(System.currentTimeMillis())
                    .action("_login")
                    .msgId("msg-" + System.currentTimeMillis())
                    .build());
        }

        log.info("[LOGIN-V2-RESOLVER] Resolved request: authType={}, tenantId={}, mobile={}, aadhaar={}, familyId={}",
                target.getAuthType(),
                target.getTenantId(),
                maskMobile(target.getMobileNumber()),
                maskAadhaar(target.getAadhaarNumber()),
                target.getFamilyId());

        return target;
    }

    /**
     * Extracts client IP from proxy headers (X-Forwarded-For, X-Real-IP) or remote socket address.
     *
     * @param request HTTP request
     * @return client IP address string
     */
    public String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "UNKNOWN";
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.isNotBlank(xRealIp)) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Extracts query parameters and form parameters from the servlet request to populate any missing
     * attributes on the target LoginV2Request.
     *
     * @param target  the LoginV2Request to enrich
     * @param request the HttpServletRequest containing query/form parameters
     */
    private void enrichFromParameters(LoginV2Request target, HttpServletRequest request) {
        if (StringUtils.isBlank(target.getTenantId()) && request.getParameter("tenantId") != null) {
            target.setTenantId(request.getParameter("tenantId").trim());
        }
        if (StringUtils.isBlank(target.getMobileNumber()) && request.getParameter("mobileNumber") != null) {
            target.setMobileNumber(request.getParameter("mobileNumber").trim());
        }
        if (StringUtils.isBlank(target.getOtp()) && request.getParameter("otp") != null) {
            target.setOtp(request.getParameter("otp").trim());
        }
        if (StringUtils.isBlank(target.getAadhaarNumber()) && request.getParameter("aadhaarNumber") != null) {
            target.setAadhaarNumber(request.getParameter("aadhaarNumber").trim());
        }
        if (StringUtils.isBlank(target.getTxnId()) && request.getParameter("txnId") != null) {
            target.setTxnId(request.getParameter("txnId").trim());
        }
        if (StringUtils.isBlank(target.getReferenceId()) && request.getParameter("referenceId") != null) {
            target.setReferenceId(request.getParameter("referenceId").trim());
        }
        if (StringUtils.isBlank(target.getFamilyId()) && request.getParameter("familyId") != null) {
            target.setFamilyId(request.getParameter("familyId").trim());
        }
        if (StringUtils.isBlank(target.getName()) && request.getParameter("name") != null) {
            target.setName(request.getParameter("name").trim());
        }
        if (StringUtils.isBlank(target.getScope()) && request.getParameter("scope") != null) {
            target.setScope(request.getParameter("scope").trim());
        }
        if (StringUtils.isBlank(target.getUserType()) && request.getParameter("userType") != null) {
            target.setUserType(request.getParameter("userType").trim());
        }
        if (target.getAuthType() == null && request.getParameter("authType") != null) {
            try {
                target.setAuthType(AuthType.fromValue(request.getParameter("authType")));
            } catch (Exception e) {
                log.warn("[LOGIN-V2-RESOLVER] Could not parse authType query parameter '{}': {}",
                        request.getParameter("authType"), e.getMessage());
            }
        }
        if (StringUtils.isBlank(target.getSelectedUserUuid()) && request.getParameter("selectedUserUuid") != null) {
            target.setSelectedUserUuid(request.getParameter("selectedUserUuid").trim());
        } else if (StringUtils.isBlank(target.getSelectedUserUuid()) && request.getParameter("userUuid") != null) {
            target.setSelectedUserUuid(request.getParameter("userUuid").trim());
        } else if (StringUtils.isBlank(target.getSelectedUserUuid()) && request.getParameter("uuid") != null) {
            target.setSelectedUserUuid(request.getParameter("uuid").trim());
        }
    }

    /**
     * Masks a mobile number showing only the first two and last two digits.
     *
     * @param mobile the raw mobile number
     * @return masked mobile string
     */
    private String maskMobile(String mobile) {
        if (StringUtils.isBlank(mobile)) return "N/A";
        if (mobile.length() >= 6) {
            return mobile.substring(0, 2) + "******" + mobile.substring(mobile.length() - 2);
        }
        return "******";
    }

    /**
     * Masks an Aadhaar number showing only the last four digits.
     *
     * @param aadhaar the raw Aadhaar number
     * @return masked Aadhaar string
     */
    private String maskAadhaar(String aadhaar) {
        if (StringUtils.isBlank(aadhaar)) return "N/A";
        String clean = aadhaar.trim().replaceAll("\\s+", "");
        if (clean.length() == 12) {
            return "XXXXXXXX" + clean.substring(8);
        }
        return "XXXXXXXX" + (clean.length() >= 4 ? clean.substring(clean.length() - 4) : "****");
    }
}
