package org.upyog.family.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Vehicle asset declaration model for a family member in UPYOG.
 *
 * <p>Encapsulates motor vehicle registration numbers, validation with national VAHAN registry,
 * vehicle class classifications, and Registration Certificate (RC) proofs.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVehicleDetail {

    /**
     * Unique internal UUID primary key for the vehicle record.
     */
    @JsonProperty("id")
    private String id;

    /**
     * Citizen {@code uuid} of the family member owning the vehicle.
     */
    @JsonProperty("userUuid")
    private String userUuid;

    /**
     * Tenant identifier inherited from the parent household unit.
     */
    @JsonProperty("tenantId")
    private String tenantId;

    /**
     * Motor vehicle registration plate number (e.g. {@code DL01AB1234}).
     */
    @JsonProperty("vehicleNumber")
    private String vehicleNumber;

    /**
     * Purchase date formatted as {@code YYYY-MM-DD}.
     */
    @JsonProperty("purchaseDate")
    private String purchaseDate;

    /**
     * Flag indicating whether the vehicle ownership is digitally authenticated against VAHAN.
     */
    @JsonProperty("isVerified")
    private Boolean isVerified;

    /**
     * Verification registry source: {@code VAHAN}, {@code TRANSPORT_DEPT}, or {@code MANUAL}.
     */
    @JsonProperty("validationSource")
    private String validationSource;

    /**
     * Vehicle category code: {@code 2_WHEELER}, {@code 4_WHEELER}, {@code COMMERCIAL}, etc.
     */
    @JsonProperty("vehicleType")
    private String vehicleType;

    /**
     * Vehicle manufacturer and model description (e.g. {@code Maruti Brezza ZXi}).
     */
    @JsonProperty("makerModel")
    private String makerModel;

    /**
     * Fuel type of the vehicle (e.g. PETROL, DIESEL, ELECTRIC, CNG, HYBRID).
     */
    @JsonProperty("fuelType")
    private String fuelType;

    /**
     * Vehicle classification category (e.g. 2_WHEELER, 4_WHEELER, COMMERCIAL, LMV, HMV).
     */
    @JsonProperty("category")
    private String category;

    /**
     * Attached Registration Certificate (RC) proof document.
     */
    @Valid
    @JsonProperty("rcDocument")
    private UserDocument rcDocument;

    /**
     * Dynamic JSON metadata for insurance or fitness certificate attributes.
     */
    @JsonProperty("additionalDetails")
    private Object additionalDetails;

    /**
     * Standard UPYOG audit metadata tracking creation and modification actor UUIDs and timestamps.
     */
    @JsonProperty("auditDetails")
    private AuditDetails auditDetails;
}
