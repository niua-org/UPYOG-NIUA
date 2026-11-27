package org.egov.user.web.contract.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.domain.model.asset.UserVehicle;
import org.egov.user.domain.service.utils.UserConstants;

/**
 * Request payload envelope for standalone vehicle CRUD operations (create, update, delete/deactivate).
 *
 * <p>Supports passing either a composite {@link UserVehicle} or flat vehicle fields
 * for seamless integration with member editing forms and administrative dashboards.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Schema(description = "Request envelope for standalone vehicle CRUD and lifecycle mutations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class VehicleRequest {

    /**
     * Standard UPYOG RequestInfo contract envelope.
     */
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    /**
     * Complete vehicle entity representing the citizen declared vehicle asset.
     */
    @Valid
    @JsonProperty("vehicle")
    private UserVehicle vehicle;

    /**
     * Citizen {@code uuid} owning this vehicle.
     */
    @JsonProperty("userUuid")
    private String userUuid;

    /**
     * Alternate citizen UUID alias.
     */
    @JsonProperty("uuid")
    private String uuid;

    /**
     * Vehicle record primary key identifier (required for update and delete/deactivate operations).
     */
    @JsonProperty("vehicleId")
    private String vehicleId;

    /**
     * Alternate primary key ID alias.
     */
    @JsonProperty("id")
    private String id;

    /**
     * Tenant identifier scoping the vehicle record.
     */
    @JsonProperty("tenantId")
    private String tenantId;

    /**
     * Motor vehicle registration plate number (e.g. DL01AB1234).
     */
    @JsonProperty("vehicleNumber")
    private String vehicleNumber;

    /**
     * Purchase date formatted as {@code YYYY-MM-DD} or epoch millisecond string.
     */
    @JsonProperty("purchaseDate")
    private String purchaseDate;

    /**
     * Flag indicating whether vehicle is verified with national VAHAN registry.
     */
    @JsonProperty("isVerified")
    private Boolean isVerified;

    /**
     * Verification source registry: {@code VAHAN}, {@code TRANSPORT_DEPT}, or {@code MANUAL}.
     */
    @JsonProperty("validationSource")
    private String validationSource;

    /**
     * Foreign key reference to Registration Certificate (RC) document in {@code ug_user_document}.
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
     * Lifecycle status override (e.g. {@code ACTIVE} or {@code INACTIVE}).
     */
    @JsonProperty("status")
    private String status;

    /**
     * Dynamic extensible JSON metadata stored as JSONB.
     */
    @JsonProperty("additionalDetails")
    private Object additionalDetails;

    /**
     * Resolves the target citizen {@code userUuid} from available contract properties.
     *
     * @return resolved citizen UUID, or null if unspecified
     */
    public String resolveUserUuid() {
        if (StringUtils.isNotBlank(userUuid)) {
            return userUuid.trim();
        }
        if (StringUtils.isNotBlank(uuid)) {
            return uuid.trim();
        }
        if (vehicle != null && StringUtils.isNotBlank(vehicle.getUserUuid())) {
            return vehicle.getUserUuid().trim();
        }
        return null;
    }

    /**
     * Resolves the target vehicle primary key ID from available contract properties.
     *
     * @return resolved vehicle ID, or null if unspecified
     */
    public String resolveVehicleId() {
        if (StringUtils.isNotBlank(vehicleId)) {
            return vehicleId.trim();
        }
        if (StringUtils.isNotBlank(id)) {
            return id.trim();
        }
        if (vehicle != null && StringUtils.isNotBlank(vehicle.getId())) {
            return vehicle.getId().trim();
        }
        return null;
    }

    /**
     * Resolves and normalizes the composite {@link UserVehicle} entity, merging flat top-level
     * fields into the nested vehicle entity if necessary.
     *
     * @return populated {@link UserVehicle} domain entity
     */
    public UserVehicle resolveVehicle() {
        UserVehicle target = (vehicle != null) ? vehicle : new UserVehicle();

        String resolvedId = resolveVehicleId();
        if (StringUtils.isNotBlank(resolvedId)) {
            target.setId(resolvedId);
        }

        String resolvedUuid = resolveUserUuid();
        if (StringUtils.isNotBlank(resolvedUuid)) {
            target.setUserUuid(resolvedUuid);
        }

        if (StringUtils.isNotBlank(tenantId) && StringUtils.isBlank(target.getTenantId())) {
            target.setTenantId(tenantId.trim());
        }
        if (StringUtils.isNotBlank(vehicleNumber) && StringUtils.isBlank(target.getVehicleNumber())) {
            target.setVehicleNumber(vehicleNumber.trim());
        }
        if (StringUtils.isNotBlank(purchaseDate) && StringUtils.isBlank(target.getPurchaseDate())) {
            target.setPurchaseDate(purchaseDate.trim());
        }
        if (isVerified != null && target.getIsVerified() == null) {
            target.setIsVerified(isVerified);
        }
        if (StringUtils.isNotBlank(validationSource) && StringUtils.isBlank(target.getValidationSource())) {
            target.setValidationSource(validationSource.trim());
        }
        if (StringUtils.isNotBlank(rcDocId) && StringUtils.isBlank(target.getRcDocId())) {
            target.setRcDocId(rcDocId.trim());
        }
        if (rcDocument != null && target.getRcDocument() == null) {
            target.setRcDocument(rcDocument);
        }
        if (StringUtils.isNotBlank(vehicleType) && StringUtils.isBlank(target.getVehicleType())) {
            target.setVehicleType(vehicleType.trim());
        }
        if (StringUtils.isNotBlank(makerModel) && StringUtils.isBlank(target.getMakerModel())) {
            target.setMakerModel(makerModel.trim());
        }
        if (StringUtils.isNotBlank(fuelType) && StringUtils.isBlank(target.getFuelType())) {
            target.setFuelType(fuelType.trim());
        }
        if (StringUtils.isNotBlank(category) && StringUtils.isBlank(target.getCategory())) {
            target.setCategory(category.trim());
        }
        if (StringUtils.isNotBlank(status)) {
            target.setStatus(status.trim().toUpperCase());
        } else if (StringUtils.isBlank(target.getStatus())) {
            target.setStatus(UserConstants.STATUS_ACTIVE_UPPERCASE);
        }
        if (additionalDetails != null && target.getAdditionalDetails() == null) {
            target.setAdditionalDetails(additionalDetails);
        }

        return target;
    }
}
