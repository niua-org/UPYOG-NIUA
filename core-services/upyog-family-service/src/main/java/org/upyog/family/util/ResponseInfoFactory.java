package org.upyog.family.util;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.springframework.stereotype.Component;

/**
 * Factory creating UPYOG ResponseInfo contract instances from RequestInfo context.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
public class ResponseInfoFactory {

    /**
     * Creates a standardized UPYOG ResponseInfo contract based on an incoming RequestInfo envelope.
     * Mirrors apiId, ver, ts, and action, and generates or propagates the msgId.
     *
     * @param requestInfo incoming request context envelope
     * @param success     boolean indicating operation outcome status
     * @return standardized ResponseInfo instance
     */
    public ResponseInfo createResponseInfoFromRequestInfo(RequestInfo requestInfo, Boolean success) {
        String apiId = requestInfo != null ? requestInfo.getApiId() : "";
        String ver = requestInfo != null ? requestInfo.getVer() : "";
        Long ts;
        if (requestInfo != null && requestInfo.getTs() != null) {
            ts = requestInfo.getTs();
        } else {
            ts = System.currentTimeMillis();
        }
        String msgId = requestInfo != null ? requestInfo.getMsgId() : "";
        String resMsgId = "uief-family-res-" + System.currentTimeMillis();
        String status = Boolean.TRUE.equals(success) ? "successful" : "failed";

        return ResponseInfo.builder()
                .apiId(apiId)
                .ver(ver)
                .ts(ts)
                .resMsgId(resMsgId)
                .msgId(msgId)
                .status(status)
                .build();
    }
}
