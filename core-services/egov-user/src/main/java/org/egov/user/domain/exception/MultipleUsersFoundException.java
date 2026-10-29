package org.egov.user.domain.exception;

import lombok.Getter;
import org.egov.user.web.contract.multiauth.UserAccountSummary;
import org.springframework.security.core.AuthenticationException;

import java.util.Collections;
import java.util.List;

/**
 * Thrown during authentication when multiple citizen accounts are linked to the same mobile number,
 * signaling the client to present an account selector.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
public class MultipleUsersFoundException extends AuthenticationException {

    private static final long serialVersionUID = 1L;

    private final String mobileNumber;
    private final List<UserAccountSummary> userAccounts;

    /**
     * Constructs a {@link MultipleUsersFoundException}.
     *
     * @param mobileNumber the mobile number linked to multiple accounts
     * @param userAccounts list of user account summaries
     */
    public MultipleUsersFoundException(String mobileNumber, List<UserAccountSummary> userAccounts) {
        super("Multiple user accounts found for mobile number: " + mobileNumber);
        this.mobileNumber = mobileNumber;
        this.userAccounts = userAccounts != null ? userAccounts : Collections.emptyList();
    }
}
