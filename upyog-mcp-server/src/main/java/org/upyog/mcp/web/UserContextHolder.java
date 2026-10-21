package org.upyog.mcp.web;

/**
 * Request-scoped holder for the user validated by {@link AuthTokenFilter}.
 * Cleared at the end of the servlet request so a later call cannot reuse the identity.
 */
public final class UserContextHolder {

    private static final ThreadLocal<AuthenticatedUser> CURRENT = new ThreadLocal<>();

    private UserContextHolder() {
    }

    /**
     * Binds the authenticated caller for the current servlet request thread.
     *
     * @param user identity loaded by {@link AuthTokenFilter}; must not be {@code null}
     */
    public static void set(AuthenticatedUser user) {
        CURRENT.set(user);
    }

    /**
     * Returns the caller bound on this thread, or {@code null} when no filter ran (for example outside {@code /mcp}).
     */
    public static AuthenticatedUser get() {
        return CURRENT.get();
    }

    /** Removes the thread-local user so pool threads cannot leak identity across requests. */
    public static void clear() {
        CURRENT.remove();
    }
}
