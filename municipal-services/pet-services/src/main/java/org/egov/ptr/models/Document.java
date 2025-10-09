package org.egov.ptr.models;

import jakarta.validation.constraints.Size;

import org.egov.ptr.validator.SanitizeHtml;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = { "filestoreid", "documentuid", "id" })
public class Document {

	@Size(max = 64)
	@SanitizeHtml
	@JsonProperty("id")
	private String id;

	@JsonProperty("active")
	private Boolean active;

	@Size(max = 64)
	@SanitizeHtml
	@JsonProperty("tenantId")
	private String tenantId = null;

	@Size(max = 64)
	@SanitizeHtml
	@JsonProperty("documentType")
	private String documentType = null;

	@Size(max = 64)
	@SanitizeHtml
	@JsonProperty("filestoreId")
	private String filestoreId = null;

	@Size(max = 64)
	@SanitizeHtml
	@JsonProperty("documentUid")
	private String documentUid;

	@JsonProperty("auditDetails")
	private AuditDetails auditDetails = null;

}
