package org.egov.userevent.model;

import java.util.List;

import org.egov.userevent.model.enums.Source;
import org.egov.userevent.model.enums.Status;
import org.egov.userevent.web.contract.Action;
import org.egov.userevent.web.contract.Event;
import org.egov.userevent.web.contract.EventDetails;
import org.egov.userevent.web.contract.Recepient;
import org.egov.userevent.web.contract.Event.EventBuilder;
import org.egov.userevent.validation.SanitizeHtml;
import org.springframework.validation.annotation.Validated;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Validated
@AllArgsConstructor
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Setter
@ToString
@Builder
public class Document {

	@SanitizeHtml
	private String documentType;

	@SanitizeHtml
	private String fileName;

	@SanitizeHtml
	private String fileStoreId;

}
