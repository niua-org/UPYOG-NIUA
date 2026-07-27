// Keep every citizen onboarding destination in one place so the feature flag
// selects a complete V1 or V2 flow without scattering version checks.
export const CITIZEN_BASE_PATH = "/upyog-ui/citizen";
export const CITIZEN_V2_ONBOARDING_STORAGE_KEY = "CITIZEN.V2.ONBOARDING";
export const CITIZEN_DASHBOARD_PATH = `${CITIZEN_BASE_PATH}/home`;

const V1_ONBOARDING_PATHS = Object.freeze({
  entry: `${CITIZEN_BASE_PATH}/select-language`,
  login: `${CITIZEN_BASE_PATH}/login`,
  register: `${CITIZEN_BASE_PATH}/register`,
});

const V2_ONBOARDING_PATHS = Object.freeze({
  entry: `${CITIZEN_BASE_PATH}/v2/login`,
  login: `${CITIZEN_BASE_PATH}/v2/login`,
  register: `${CITIZEN_BASE_PATH}/v2/register`,
  loginOtp: `${CITIZEN_BASE_PATH}/v2/login/otp`,
  registerOtp: `${CITIZEN_BASE_PATH}/v2/register/otp`,
});

// Return one immutable route set for the active onboarding version. Callers
// derive it once and reuse it for entry, login, registration, OTP and fallback navigation.
export const getCitizenOnboardingPaths = (isV2) => (isV2 ? V2_ONBOARDING_PATHS : V1_ONBOARDING_PATHS);

// OTP is valid only for the two supported V2 entry flows. This keeps a route
// such as /v2/unknown/otp from being treated as a real onboarding deep link.
export const isValidCitizenV2Flow = (flow) => flow === "login" || flow === "register";

// Never send an authenticated V2 citizen back into an onboarding URL. Module
// deep links remain valid destinations; onboarding/base paths resolve to home.
export const getCitizenPostAuthPath = (requestedPath) => {
  const onboardingPrefixes = [
    `${CITIZEN_BASE_PATH}/v2`,
    `${CITIZEN_BASE_PATH}/login`,
    `${CITIZEN_BASE_PATH}/register`,
  ];
  if (
    !requestedPath ||
    requestedPath === CITIZEN_BASE_PATH ||
    requestedPath.startsWith(`${CITIZEN_BASE_PATH}/select-`) ||
    onboardingPrefixes.some((path) => requestedPath === path || requestedPath.startsWith(`${path}/`))
  ) {
    return CITIZEN_DASHBOARD_PATH;
  }
  return requestedPath;
};
