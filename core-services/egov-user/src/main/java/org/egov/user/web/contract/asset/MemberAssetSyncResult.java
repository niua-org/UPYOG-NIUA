package org.egov.user.web.contract.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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
     * Number of documents created or updated.
     */
    @JsonProperty("documentsSynced")
    private int documentsSynced;
}
