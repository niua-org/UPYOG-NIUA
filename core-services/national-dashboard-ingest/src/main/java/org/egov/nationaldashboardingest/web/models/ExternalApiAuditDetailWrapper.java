package org.egov.nationaldashboardingest.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Existing root JSON object expected by {@code external-api-audit-persister.yml}
 * ({@code basePath: $.apiAuditDetail}). JSON property name is unchanged.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ExternalApiAuditDetailWrapper {

    @JsonProperty("apiAuditDetail")
    private ExternalApiAuditDetail apiAuditDetail;
}
