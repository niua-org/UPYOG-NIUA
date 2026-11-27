package org.egov.user.web.contract.auth;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Contract representation of an authenticated user principal serialized and cached in Redis.
 *
 * <p>Implements {@link AuthUserAccount} and {@link Serializable} to serve as the unified authentication
 * token details object across Spring Authorization Server, Redis token store, and downstream API gateway filters.</p>
 *
 * <p><b>Key Responsibilities & Architectural Constraints:</b></p>
 * <ul>
 *   <li><b>Redis Principal Caching:</b> Preserves authenticated citizen/employee state across distributed
 *       gateway requests without repeated database round-trips. Changes to class structure require strict
 *       {@link #serialVersionUID} compatibility checks to prevent token deserialization failures.</li>
 *   <li><b>Identity & Demographic Context:</b> Encapsulates verified identity metadata including
 *       e-KYC verification status ({@code isIdentityVerified}), verification timestamp, verification source
 *       (e.g. Aadhaar, DigiLocker), and family unit linkage ({@code familyId}).</li>
 *   <li><b>RBAC Role Guarantees:</b> {@link #getRoles()} guarantees non-null collection semantics to ensure
 *       fail-closed evaluation in downstream gateway authorization filters.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 * @see AuthUserAccount
 * @see Role
 */
@Setter
@Getter
@Builder
public class User implements Serializable, AuthUserAccount {
    private static final long serialVersionUID = -1053170163821651014L;
    private Long id;
    private String uuid;
    private String userName;
    private String name;
    private String mobileNumber;
    private String emailId;
    private String locale;
    private String type;
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
    private boolean active;
    private String tenantId;
    private String permanentCity;
    private String fatherName;
    private String motherName;
    private String spouseName;
    private Boolean isIdentityVerified;
    private Long identityVerifiedDate;
    private String identityVerifiedSource;
    private Long lastResyncDate;
    private Integer edittedCount;
    private String lastLoginMethod;
    private Boolean isProfileCompleted;
    private String familyId;

    /**
     * CRITICAL: Override getRoles to ensure it never returns null.
     * This prevents validation errors in gateway RBAC filter.
     *
     * @return non-null set of user roles
     */
    public Set<Role> getRoles() {
        if (roles == null) {
            roles = new HashSet<>();
        }
        return roles;
    }
}