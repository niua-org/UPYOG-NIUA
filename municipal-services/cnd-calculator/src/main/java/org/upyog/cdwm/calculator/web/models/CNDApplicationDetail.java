package org.upyog.cdwm.calculator.web.models;

import java.math.BigDecimal;

import org.egov.common.contract.request.User;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Request schema of CND application.  
 */
@Validated
@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2020-12-23T12:08:13.326Z[GMT]")

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class CNDApplicationDetail   {
  @JsonProperty("citizen")
  private User citizen = null;

  @JsonProperty("id")
  private String id = null;

  @JsonProperty("tenantId")
  private String tenantId = null;

  @JsonProperty("applicationNumber")
  private String applicationNumber = null;

  @JsonProperty("applicationStatus")
  private String applicationStatus = null;
  
  @JsonProperty("totalWasteQuantity")
  private BigDecimal totalWasteQuantity;

  @JsonProperty("noOfTrips")
  private Integer noOfTrips = null;
  
  @JsonProperty("vehicleType")
  private String vehicleType = null;
  

  /**
   * Gets or Sets status
   */
  public enum StatusEnum {
    ACTIVE("ACTIVE"),
    
    INACTIVE("INACTIVE"),
	  
	SUCCESSFUL("SUCCESSFUL");  

    private String value;

    StatusEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static StatusEnum fromValue(String text) {
      for (StatusEnum b : StatusEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }
  @JsonProperty("status")
  private StatusEnum status = null;

  @JsonProperty("vehicleId")
  private String vehicleId = null;
  
  @JsonProperty("auditDetails")
  private AuditDetails auditDetails = null;

  
}
