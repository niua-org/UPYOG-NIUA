import { persistAuthSession } from "../../onboarding/authSession";

// The backend expects the lower-case citizen type in OTP and password-grant
// requests. Keep it centralized so V1, V2 and DigiLocker use the same value.
const CITIZEN_USER_TYPE = "citizen";

// DigiLocker APIs identify this authentication integration as the SSO module.
const DIGILOCKER_MODULE = "SSO";

// OAuth requires the token exchange redirect URI to exactly match the URI used
// when authorization started. This legacy registered URI is therefore retained
// even when V2 receives and completes the callback on its login route.
const DEFAULT_DIGILOCKER_REDIRECT_URI = "https://upyog-test.niua.org/upyog-ui/citizen/login/otp";

// The OTP service uses this business code when login is attempted for a mobile
// number that does not yet have a citizen account.
const USER_NOT_REGISTERED_OTP_CODE = "OTP.UNKNOWN_CREDENTIAL";

// The OTP service uses this code when a registration attempt finds an existing
// account; DigiLocker then switches from registration OTP to login OTP.
const USER_ALREADY_REGISTERED_OTP_CODE = "OTP.MOBILENUMBER";

// OTP failures have appeared in both legacy `error.fields` and newer `Errors`
// envelopes, so read both without coupling callers to a particular response.
const getOtpErrorCode = (error) =>
  error?.response?.data?.error?.fields?.[0]?.code ||
  error?.response?.data?.Errors?.[0]?.code;

// OTP business errors control the login/register branch. Transport and server
// failures must remain visible API errors instead of changing the user flow.
export const isCitizenNotRegisteredError = (error) =>
  getOtpErrorCode(error) === USER_NOT_REGISTERED_OTP_CODE;

// This check is intentionally private because only the DigiLocker registration
// probe needs to distinguish an existing citizen from other OTP failures.
const isCitizenAlreadyRegisteredError = (error) =>
  getOtpErrorCode(error) === USER_ALREADY_REGISTERED_OTP_CODE;

// V1 identifies existing users by whether the login OTP request succeeds. V2
// reuses the same request shape and error semantics instead of adding a new API.
// `details` carries registration-only values such as name and date of birth;
// login callers can omit them and still use this single request builder.
export const sendCitizenOtp = ({ stateCode, flow, mobileNumber, ...details }) =>
  Digit.UserService.sendOtp(
    {
      otp: {
        // Preserve optional flow-specific fields without changing the common
        // OTP contract shared by login, registration and resend operations.
        ...details,
        mobileNumber,
        // Citizen authentication remains state-scoped even when the selected
        // home city points at a more specific tenant.
        tenantId: stateCode,
        userType: CITIZEN_USER_TYPE,
        // The service recognizes `login`, `register`, and other OTP operations
        // through this type field.
        type: flow,
      },
    },
    // The service wrapper also requires the tenant as a separate argument.
    stateCode
  );

// Exchange a successfully verified login OTP through the existing password
// grant, then normalize the service response into the application's user shape.
export const authenticateCitizen = async ({ stateCode, mobileNumber, otp }) => {
  // ResponseInfo is request metadata and must not be stored in the user session.
  // UserRequest becomes `info`; all remaining fields contain access tokens.
  const { ResponseInfo, UserRequest: info, ...tokens } = await Digit.UserService.authenticate({
    // Citizen OTP login treats the submitted mobile number as the username and
    // the verified OTP as the temporary password.
    username: mobileNumber,
    password: otp,
    tenantId: stateCode,
    userType: CITIZEN_USER_TYPE,
  });

  // Preserve the existing single-instance tenant normalization for both UIs.
  if (window?.globalConfigs?.getConfig("ENABLE_SINGLEINSTANCE")) {
    info.tenantId = Digit.ULBService.getStateId();
  }
  // Match the object structure expected by Digit.UserService.setUser.
  return { info, ...tokens };
};

// Create a new Citizen account after registration OTP verification and return
// the same normalized session shape produced by authenticateCitizen.
export const registerCitizen = async ({ stateCode, mobileNumber, otp, name }) => {
  // ResponseInfo is excluded for the same reason as login: it is not user or
  // token state consumed by downstream modules.
  const { ResponseInfo, UserRequest: info, ...tokens } = await Digit.UserService.registerUser(
    {
      name,
      // Citizen registration also uses the mobile number as the account name.
      username: mobileNumber,
      // The registration API calls the submitted OTP an otpReference.
      otpReference: otp,
      tenantId: stateCode,
    },
    stateCode
  );

  // Keep registration tenant normalization identical to OTP login in
  // single-instance deployments.
  if (window?.globalConfigs?.getConfig("ENABLE_SINGLEINSTANCE")) {
    info.tenantId = Digit.ULBService.getStateId();
  }
  // Consumers can persist login and registration results through one helper.
  return { info, ...tokens };
};

// Persist the same UPYOG and backward-compatible citizen session keys used by
// V1 so downstream modules cannot distinguish a V2-authenticated citizen.
export const persistCitizenSession = (user, tenantId) => {
  // Reuse the language selected during initialization so authenticated screens
  // continue with the same locale after the full-page onboarding redirect.
  const locale = JSON.parse(sessionStorage.getItem("Digit.initData"))?.value?.selectedLanguage;

  // The shared helper writes generic compatibility keys plus Citizen aliases.
  persistAuthSession({
    user,
    tenantId,
    locale,
    storageNamespace: "Citizen",
  });
};

// MDMS/configured select values may represent a tenant as a complete option, a
// raw code, or an object using a legacy value/tenantId property.
export const getCitizenTenantCode = (city) => {
  // Raw strings are already tenant codes and can be returned unchanged.
  if (!city || typeof city !== "object") return city;
  // Prefer the normalized `code`, then support the two legacy aliases.
  return city.code || city.value || city.tenantId;
};

// V2 collects the home city before authentication, so persist it through the
// same session key consumed by ULBService and the citizen dashboard.
export const persistCitizenHomeCity = (city) => {
  const code = getCitizenTenantCode(city);
  // Do not overwrite a valid selected city with incomplete configuration data.
  if (!code) return;
  // ULBService expects an object. Preserve the original option metadata when it
  // exists while ensuring every stored representation contains `code`.
  Digit.SessionStorage.set("CITIZEN.COMMON.HOME.CITY", typeof city === "object" ? { ...city, code } : { code });
};

// Reuse the V1 DigiLocker authorization endpoint and verifier storage. The
// browser redirect remains owned by the existing DigiLocker service response.
export const startCitizenDigiLockerLogin = async () => {
  // The register call starts OAuth/PKCE and returns both the verifier reference
  // and the external DigiLocker authorization URL.
  const data = await Digit.DigiLockerService.register({ module: DIGILOCKER_MODULE });
  // Persist the verifier reference across the full-page external redirect so
  // the callback can prove it belongs to this authorization attempt.
  localStorage.setItem("code_verfier_register", data?.dlReqRef);
  // Leave the application and begin user authorization on DigiLocker's page.
  window.location.assign(data.redirectURL);
};

// DigiLocker returns DOB as DDMMYYYY, while Citizen registration expects
// DD/MM/YYYY. Unknown or already formatted values pass through unchanged.
const formatDigiLockerDate = (dateValue) => {
  if (!dateValue || dateValue.length !== 8) return dateValue;
  return `${dateValue.slice(0, 2)}/${dateValue.slice(2, 4)}/${dateValue.slice(4, 8)}`;
};

// Complete the same token -> user-check -> OAuth sequence currently used by
// V1. The original redirect URI is retained because it must match the OAuth request.
export const completeCitizenDigiLockerLogin = async ({ code, stateCode }) => {
  // Exchange the authorization code with the verifier created before redirect.
  const tokenData = await Digit.DigiLockerService.token({
    TokenReq: {
      code_verifier: localStorage.getItem("code_verfier_register"),
      code,
      module: DIGILOCKER_MODULE,
      // This value must match the URI registered for the original OAuth request.
      redirect_uri: DEFAULT_DIGILOCKER_REDIRECT_URI,
    },
  });
  // TokenRes contains the verified DigiLocker identity and OAuth access token.
  const tokenResponse = tokenData?.TokenRes;

  // Normalize the DigiLocker identity into the fields used by the Citizen OTP
  // and registration services.
  const citizenDetails = {
    dob: formatDigiLockerDate(tokenResponse?.dob),
    mobileNumber: tokenResponse?.mobile,
    name: tokenResponse?.name,
  };

  // V1 first tries registration OTP and falls back to login OTP when the user
  // already exists; retain that behavior before completing DigiLocker OAuth.
  try {
    await sendCitizenOtp({ stateCode, flow: "register", ...citizenDetails });
  } catch (registrationError) {
    // Only the explicit "already registered" response may switch to login.
    // Network, validation and server failures must still reject the flow.
    if (!isCitizenAlreadyRegisteredError(registrationError)) throw registrationError;
    await sendCitizenOtp({ stateCode, flow: "login", mobileNumber: citizenDetails.mobileNumber });
  }

  // Ask the backend to create/authenticate the local Citizen represented by the
  // verified DigiLocker identity.
  const authData = await Digit.DigiLockerService.oauth({
    access_token: tokenResponse?.access_token,
    tenantId: stateCode,
    digilockerid: tokenResponse?.digilockerid,
    name: tokenResponse?.name,
    dob: citizenDetails.dob,
    gender: tokenResponse?.gender,
    mobileNumber: tokenResponse?.mobile,
  });
  // Normalize DigiLocker OAuth output to the same { info, ...tokens } contract
  // returned by OTP login and registration.
  const { UserRequest: info, ...tokens } = authData;
  return { info, ...tokens };
};

// Convert supported API error envelopes into the message consumed by Toast.
// Prefer backend-specific messages and use the caller's localized/general
// fallback only when the error does not contain a useful description.
export const getCitizenAuthError = (error, fallback = "Unable to complete authentication. Please try again.") => {
  return error?.response?.data?.Errors?.[0]?.message || error?.response?.data?.error_description || error?.message || fallback;
};
