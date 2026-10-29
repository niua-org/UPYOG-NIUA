package org.egov.user.web.contract.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.user.domain.model.UserDocument;

import java.util.List;

/**
 * Standard response envelope for citizen document operations.
 */
@Schema(description = "Response envelope containing citizen document details and resolved file download URLs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDocumentResponse {

    @Schema(description = "Standard UPYOG ResponseInfo contract")
    @JsonProperty("responseInfo")
    private ResponseInfo responseInfo;

    @Schema(description = "List of citizen documents")
    @JsonProperty("documents")
    private List<UserDocument> documents;
}
