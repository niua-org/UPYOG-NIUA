package org.egov.user.domain.model.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.egov.user.domain.model.UserDocument;

/**
 * Domain entity representing citizen motor vehicle ownership registry in {@code ug_user_vehicle}.
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
public class UserVehicle {

    /**
     * Unique internal UUID primary key for the vehicle record in {@code ug_user_vehicle}.
     */
    @JsonProperty("id")
    private String id;

    /**
     * Citizen {@code uuid} owning this vehicle.
     */
    @JsonProperty("userUuid")
    private String userUuid;

    /**
     * Tenant identifier scoping the vehicle record.
     */
    @JsonProperty("tenantId")
    private String tenantId;

    /**
     * Motor vehicle registration plate number (e.g. {@code DL01AB1234}).
     */
    @JsonProperty("vehicleNumber")
    private String vehicleNumber;

    /**
     * Purchase date formatted as {@code YYYY-MM-DD} or epoch string.
     */
    @JsonProperty("purchaseDate")
    private String purchaseDate;

    /**
     * Flag indicating whether vehicle is verified with national VAHAN registry.
     */
    @JsonProperty("isVerified")
    @Builder.Default
    private Boolean isVerified = false;

    /**
     * Verification source registry: {@code VAHAN}, {@code TRANSPORT_DEPT}, or {@code MANUAL}.
     */
    @JsonProperty("validationSource")
    private String validationSource;

    /**
     * Foreign key reference to Registration Certificate (RC) in {@code ug_user_document}.
     */
    @JsonProperty("rcDocId")
    private String rcDocId;

    /**
     * Attached RC proof document contract.
     */
    @Valid
    @JsonProperty("rcDocument")
    private UserDocument rcDocument;

    /**
     * Vehicle classification type (e.g. 2_WHEELER, 4_WHEELER, COMMERCIAL).
     */
    @JsonProperty("vehicleType")
    private String vehicleType;

    /**
     * Vehicle manufacturer and model specification.
     */
    @JsonProperty("makerModel")
    private String makerModel;

    /**
     * Lifecycle status of the vehicle record (defaults to {@code ACTIVE}).
     */
    @JsonProperty("status")
    @Builder.Default
    private String status = "ACTIVE";

    /**
     * Dynamic extensible JSON metadata stored as JSONB.
     */
    @JsonProperty("additionalDetails")
    private Object additionalDetails;

    /**
     * Actor UUID who created this record.
     */
    @JsonProperty("createdBy")
    private String createdBy;

    /**
     * Epoch millisecond timestamp of record creation.
     */
    @JsonProperty("createdTime")
    private Long createdTime;

    /**
     * Actor UUID who last modified this record.
     */
    @JsonProperty("lastModifiedBy")
    private String lastModifiedBy;

    /**
     * Epoch millisecond timestamp of last modification.
     */
    @JsonProperty("lastModifiedTime")
    private Long lastModifiedTime;
}
