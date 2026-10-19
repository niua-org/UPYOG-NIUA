package org.egov.user.web.contract.multiauth;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.egov.user.domain.model.User;

import java.io.Serializable;
import java.util.Date;

/**
 * Summary DTO of a citizen account returned when multiple profiles are linked to a single mobile number.
 * Allows the citizen to identify and select their profile before token generation.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Schema(description = "Summary of a citizen user profile for multi-account selection")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccountSummary implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Unique citizen UUID", example = "e5b881ef-2d88-4c91-b3b3-df7b06b6a482")
    @JsonProperty("uuid")
    private String uuid;

    @Schema(description = "Citizen database ID", example = "1052")
    @JsonProperty("id")
    private Long id;

    @Schema(description = "Citizen full name", example = "Aarav Sharma")
    @JsonProperty("name")
    private String name;

    @Schema(description = "Citizen username", example = "9876543210")
    @JsonProperty("userName")
    private String userName;

    @Schema(description = "Citizen mobile number", example = "9876543210")
    @JsonProperty("mobileNumber")
    private String mobileNumber;

    @Schema(description = "Masked Aadhaar number", example = "XXXXXXXX1234")
    @JsonProperty("aadhaarNumber")
    private String aadhaarNumber;

    @Schema(description = "Citizen gender", example = "MALE")
    @JsonProperty("gender")
    private String gender;

    @Schema(description = "Date of Birth", example = "15/08/1990")
    @JsonProperty("dob")
    @JsonFormat(pattern = "dd/MM/yyyy")
    private Date dob;

    @Schema(description = "Profile picture filestore ID or URL")
    @JsonProperty("photo")
    private String photo;

    @Schema(description = "Whether identity has been verified via Aadhaar")
    @JsonProperty("isIdentityVerified")
    private Boolean isIdentityVerified;

    @Schema(description = "Father's name")
    @JsonProperty("fatherName")
    private String fatherName;

    @Schema(description = "Mother's name")
    @JsonProperty("motherName")
    private String motherName;

    /**
     * Factory method creating a {@link UserAccountSummary} from a domain {@link User}.
     *
     * @param user the domain user
     * @return summary instance or null if user is null
     */
    public static UserAccountSummary fromUser(User user) {
        if (user == null) {
            return null;
        }
        return UserAccountSummary.builder()
                .uuid(user.getUuid())
                .id(user.getId())
                .name(user.getName())
                .userName(user.getUsername())
                .mobileNumber(user.getMobileNumber())
                .aadhaarNumber(maskAadhaar(user.getAadhaarNumber()))
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .dob(user.getDob())
                .photo(user.getPhoto())
                .isIdentityVerified(user.getIsIdentityVerified())
                .fatherName(user.getFatherName())
                .motherName(user.getMotherName())
                .build();
    }

    /**
     * Masks an Aadhaar string, preserving only the last four digits and prepending 8 mask characters.
     *
     * @param aadhaar the raw or partially masked Aadhaar number
     * @return masked Aadhaar string (e.g. XXXXXXXX1234), or null if input is blank
     */
    private static String maskAadhaar(String aadhaar) {
        if (StringUtils.isBlank(aadhaar)) return null;
        String clean = aadhaar.replaceAll("\\s+", "");
        if (clean.startsWith("XXXXXXXX") || clean.startsWith("xxxxXXXX")) {
            return clean;
        }
        return "XXXXXXXX" + (clean.length() >= 4 ? clean.substring(clean.length() - 4) : "****");
    }
}
