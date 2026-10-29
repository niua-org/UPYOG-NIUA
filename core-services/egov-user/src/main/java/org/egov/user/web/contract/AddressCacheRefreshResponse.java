package org.egov.user.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.egov.common.contract.response.ResponseInfo;

import java.util.Map;
import java.util.Set;

/**
 * Response payload returning the status and active Address Types following a cache refresh.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressCacheRefreshResponse {

    @JsonProperty("ResponseInfo")
    private ResponseInfo responseInfo;

    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("allowedAddressTypes")
    private Set<String> allowedAddressTypes;

    @JsonProperty("cachedAddressTypesByTenant")
    private Map<String, Set<String>> cachedAddressTypesByTenant;

    @JsonProperty("message")
    private String message;
}
