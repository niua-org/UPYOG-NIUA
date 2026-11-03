package org.egov.user.web.contract.profile;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.common.contract.request.RequestInfo;

import java.util.List;

/**
 * Request DTO for searching citizen profiles by identifier, mobile number, Aadhaar, or demographics.
 * Supports modular entity inclusion via the {@link #include} field.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Schema(description = "Request payload for searching citizen profiles by criteria")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileSearchRequest {

    @Schema(description = "Standard UPYOG RequestInfo contract")
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    @Schema(description = "Tenant identifier", example = "pg.citya")
    private String tenantId;

    @Schema(description = "List of citizen UUIDs to search for")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> uuid;

    @Schema(description = "List of citizen internal IDs to search for")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<Long> id;

    @Schema(description = "Citizen mobile number", example = "9876543210")
    private String mobileNumber;

    @Schema(description = "Citizen Aadhaar number", example = "XXXXXXXX1234")
    private String aadhaarNumber;

    @Schema(description = "Citizen name prefix or exact match", example = "Aarav")
    private String name;

    @Schema(description = "Active status filter (true = active only, false = inactive only, null = both)", example = "true")
    private Boolean active;

    @Schema(description = "Identity verification status filter", example = "true")
    private Boolean isIdentityVerified;

    @Schema(description = "List of entity sections to include in response (allowed values: PROFILE, ADDRESS, DOCUMENT). Defaults to [PROFILE] if omitted.", example = "[\"PROFILE\", \"ADDRESS\", \"DOCUMENT\"]")
    @JsonProperty("include")
    @JsonAlias({"flags", "entities", "requiredFields"})
    private List<String> include;
}
