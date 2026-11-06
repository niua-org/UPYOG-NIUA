package org.egov.dx.web.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Encapsulates resolved citizen account metadata from user registry.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UserRegistryRecord {

    /**
     * Unique identifier of the citizen user in egov-user.
     */
    private String userUuid;

    /**
     * Active Family ID associated with the citizen from upyog-family-service.
     */
    private String familyId;
}
