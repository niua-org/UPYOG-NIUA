package org.egov.user.web.contract.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.common.contract.request.RequestInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Request DTO for searching a citizen profile by UUID, mobile number, Aadhaar, or demographics.
 * <p>
 * Supports searching only a single user profile at a time using {@code uuid}.
 * Searching by internal {@code userId} or {@code id} is not supported.
 * Supports modular entity inclusion via the {@link #include} field using {@link ProfileIncludeEntity}.
 * </p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Schema(description = "Request payload for searching a citizen profile by criteria")
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

    @Schema(description = "Citizen UUID to search for. Must be a single UUID string (array usage is not supported).", example = "123e4567-e89b-12d3-a456-426614174000")
    @JsonProperty("uuid")
    @JsonDeserialize(using = UuidDeserializer.class)
    private String uuid;

    @JsonIgnore
    private boolean multipleUuidsProvided;

    @Schema(description = "Deprecated. Searching by userId / userIDs / id is not supported. Use uuid instead.")
    @JsonProperty("userId")
    private Object userId;

    @JsonProperty("userIDs")
    private Object userIDs;

    @JsonProperty("userIds")
    private Object userIds;

    @JsonProperty("id")
    private Object id;

    /**
     * Checks if caller supplied any legacy or unsupported user ID filter.
     *
     * @return true if userId, userIDs, userIds, or id is non-null
     */
    public boolean hasUserIdCriteria() {
        return userId != null || userIDs != null || userIds != null || id != null;
    }

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
    private List<ProfileIncludeEntity> include;

    /**
     * Helper to retrieve the single target citizen UUID.
     *
     * @return single citizen UUID if present, null otherwise
     */
    public String getSingleUuid() {
        return uuid;
    }

    public static class ProfileSearchRequestBuilder {
        public ProfileSearchRequestBuilder uuid(String singleUuid) {
            this.uuid = singleUuid != null ? singleUuid.trim() : null;
            return this;
        }

        public ProfileSearchRequestBuilder uuid(List<String> uuids) {
            if (uuids != null && uuids.size() > 1) {
                this.multipleUuidsProvided = true;
            }
            this.uuid = (uuids != null && !uuids.isEmpty()) ? String.valueOf(uuids.get(0)).trim() : null;
            return this;
        }
    }

    /**
     * Custom Jackson deserializer allowing uuid to be deserialized from either a single String or an Array.
     * When multiple elements are present in the Array, marks multipleUuidsProvided = true for validation.
     */
    public static class UuidDeserializer extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.currentToken() == JsonToken.START_ARRAY) {
                List<String> list = new ArrayList<>();
                while (p.nextToken() != JsonToken.END_ARRAY) {
                    list.add(p.getText());
                }
                Object target = p.getCurrentValue();
                if (target == null && p.getParsingContext() != null) {
                    target = p.getParsingContext().getCurrentValue();
                    if (target == null && p.getParsingContext().getParent() != null) {
                        target = p.getParsingContext().getParent().getCurrentValue();
                    }
                }
                if (target instanceof ProfileSearchRequest req) {
                    req.setMultipleUuidsProvided(true);
                }
                return list.isEmpty() ? null : list.get(0).trim();
            }
            String text = p.getText();
            return text != null ? text.trim() : null;
        }
    }
}
