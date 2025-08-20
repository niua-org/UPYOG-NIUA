package org.egov.ewst.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.egov.ewst.models.user.User;
import org.egov.ewst.models.workflow.State;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(of = { "id" })
@ToString
public class ProcessInstance {

	@Size(max = 64)
	@JsonProperty("id")
	private String id;

	@NotNull
	@Size(max = 128)
	@JsonProperty("tenantId")
	private String tenantId;

	@NotNull
	@Size(max = 128)
	@JsonProperty("businessService")
	private String businessService;

	@NotNull
	@Size(max = 128)
	@JsonProperty("businessId")
	private String businessId;

	@NotNull
	@Size(max = 128)
	@JsonProperty("action")
	private String action;

	@NotNull
	@Size(max = 64)
	@JsonProperty("moduleName")
	private String moduleName;

	@JsonProperty("state")
	private State state;

	@JsonProperty("comment")
	private String comment;

	@JsonProperty("documents")
	@Valid
	private List<Document> documents;

	@JsonProperty("assignes")
	private List<User> assignes;

	/**
	 * Adds a document to the list of documents associated with the process instance.
	 *
	 * @param documentsItem the document to add
	 * @return the updated ProcessInstance object
	 */
	public ProcessInstance addDocumentsItem(Document documentsItem) {
		if (this.documents == null) {
			this.documents = new ArrayList<>();
		}
		if (!this.documents.contains(documentsItem))
			this.documents.add(documentsItem);

		return this;
	}

}