package org.upyog.family.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Enriched citizen demographic profile resolved from UPYOG user services.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitizenProfile {

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("userName")
    private String userName;

    @JsonProperty("name")
    private String name;

    @JsonProperty("mobileNumber")
    private String mobileNumber;

    @JsonProperty("gender")
    private String gender;

    @JsonProperty("dob")
    private Long dob;

    @JsonProperty("photo")
    private String photo;

    @JsonProperty("emailId")
    private String emailId;
}
