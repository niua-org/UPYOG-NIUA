package org.egov.user.domain.service.multiauth.strategy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.egov.user.domain.model.User;
import org.egov.user.web.contract.multiauth.AuthType;

import java.util.List;

/**
 * Encapsulates the resolved domain User and metadata resulting from a strategy authentication execution.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthenticatedUserResult {
    private User user;
    private boolean isNewUser;
    private AuthType authType;
    private boolean multipleUsersFound;
    private List<User> matchedUsers;
}
