const EMPLOYEE_AUTH_BASE_PATH = "/upyog-ui/employee/user";

/**
 * Centralize employee authentication URLs so unauthenticated redirects and
 * auth-page navigation cannot accidentally cross between V1 and V2.
 */
export const getEmployeeAuthPaths = (isV2 = false) => {
  const versionSegment = isV2 ? "/v2" : "";
  const authBasePath = `${EMPLOYEE_AUTH_BASE_PATH}${versionSegment}`;

  return {
    login: `${authBasePath}/login`,
    forgotPassword: `${authBasePath}/forgot-password`,
    changePassword: `${authBasePath}/change-password`,
  };
};

// Non-sensitive Employee V2 values can survive refresh while password fields
// continue to be excluded by OnboardingProvider's persistence policy.
export const EMPLOYEE_V2_ONBOARDING_STORAGE_KEY = "EMPLOYEE.V2.ONBOARDING";
