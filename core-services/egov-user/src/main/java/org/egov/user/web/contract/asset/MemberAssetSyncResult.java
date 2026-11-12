package org.egov.user.web.contract.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Result details for an individual synchronized member record.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MemberAssetSyncResult {

    /**
     * Citizen {@code uuid} that was synchronized.
     */
    @JsonProperty("userUuid")
    private String userUuid;

    /**
     * Synchronization status: {@code SUCCESS} or {@code FAILED}.
     */
    @JsonProperty("status")
    private String status;

    /**
     * Message describing synchronization outcome or failure reason.
     */
    @JsonProperty("message")
    private String message;

    /**
     * Number of property declarations created or updated.
     */
    @JsonProperty("propertiesSynced")
    private int propertiesSynced;

    /**
     * Flag indicating whether income declaration was created or updated.
     */
    @JsonProperty("incomeSynced")
    private boolean incomeSynced;

    /**
     * Number of vehicle declarations created or updated.
     */
    @JsonProperty("vehiclesSynced")
    private int vehiclesSynced;

    /**
     * Number of documents created or updated.
     */
    @JsonProperty("documentsSynced")
    private int documentsSynced;

    /**
     * List of persisted property record IDs.
     */
    @JsonProperty("propertyIds")
    @Builder.Default
    private List<String> propertyIds = new ArrayList<>();

    /**
     * Persisted income record ID.
     */
    @JsonProperty("incomeId")
    private String incomeId;

    /**
     * List of persisted vehicle record IDs.
     */
    @JsonProperty("vehicleIds")
    @Builder.Default
    private List<String> vehicleIds = new ArrayList<>();
}
