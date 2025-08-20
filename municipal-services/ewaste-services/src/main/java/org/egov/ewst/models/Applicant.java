package org.egov.ewst.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import org.springframework.validation.annotation.Validated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;

/**
 * Represents the details of the user applying for an Ewaste application.
 * This class contains information such as the applicant's name, gender, contact details, and tenant ID.
 */
@ApiModel(description = "Details of the user applying for ewaste application")
@Validated
@jakarta.annotation.Generated(value = "org.egov.codegen.SpringBootCodegen", date = "2022-08-16T15:34:24.436+05:30")

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Applicant {
	@JsonProperty("id")
	private String id = null;

	@JsonProperty("applicantName")
	private String applicantName = null;

	@JsonProperty("gender")
	private String gender = null;

	@JsonProperty("mobileNumber")
	private String mobileNumber = null;

	@JsonProperty("emailId")
	private String emailId = null;

	@JsonProperty("altMobileNumber")
	private String altMobileNumber = null;

	@JsonProperty("tenantId")
	private String tenantId = null;

	@JsonProperty("ewId")
	private String ewId = null;
}