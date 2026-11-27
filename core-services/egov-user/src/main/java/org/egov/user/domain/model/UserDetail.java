package org.egov.user.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Domain aggregate holding authenticated user credentials alongside permitted UI/API actions.
 * <p>
 * Combines the {@link SecureUser} (principal details and granted authorities) with the complete
 * list of RBAC-permitted {@link Action} objects resolved for the user's role set in the tenant.
 */
@AllArgsConstructor
@Getter
public class UserDetail {
    private SecureUser secureUser;
    private List<Action> actions;
}
