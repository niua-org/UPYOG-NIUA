package org.egov.user.web.contract.auth;

import java.util.Set;

/**
 * Common contract abstraction for authenticated user identity across OAuth and domain layers.
 * Adheres to Interface Segregation Principle (ISP) to decouple services from concrete implementations
 * and resolve type-naming ambiguity between domain and contract User models.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface AuthUserAccount {

    /**
     * Retrieves unique universally unique identifier of the authenticated user.
     *
     * @return user UUID string
     */
    String getUuid();

    /**
     * Retrieves internal database identifier of the authenticated user.
     *
     * @return user ID long
     */
    Long getId();

    /**
     * Retrieves user classification type (e.g. CITIZEN, EMPLOYEE).
     *
     * @return user type string
     */
    String getType();

    /**
     * Retrieves primary tenant identifier associated with the user account.
     *
     * @return tenant ID string
     */
    String getTenantId();

    /**
     * Retrieves set of security roles assigned to the user account.
     *
     * @return non-null set of {@link Role} objects
     */
    Set<Role> getRoles();
}
