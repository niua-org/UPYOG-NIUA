const CITIZEN_USER_TYPE = "citizen";
const DIGILOCKER_MODULE = "SSO";
const DEFAULT_DIGILOCKER_REDIRECT_URI = "https://upyog-test.niua.org/upyog-ui/citizen/login/otp";
const USER_NOT_REGISTERED_OTP_CODE = "OTP.UNKNOWN_CREDENTIAL";
const USER_ALREADY_REGISTERED_OTP_CODE = "OTP.MOBILENUMBER";

const getOtpErrorCode = (error) =>
  error?.response?.data?.error?.fields?.[0]?.code ||
  error?.response?.data?.Errors?.[0]?.code;

// OTP business errors control the login/register branch. Transport and server
// failures must remain visible API errors instead of changing the user flow.
export const isCitizenNotRegisteredError = (error) =>
  getOtpErrorCode(error) === USER_NOT_REGISTERED_OTP_CODE;

const isCitizenAlreadyRegisteredError = (error) =>
  getOtpErrorCode(error) === USER_ALREADY_REGISTERED_OTP_CODE;

// V1 identifies existing users by whether the login OTP request succeeds. V2
// reuses the same request shape and error semantics instead of adding a new API.
export const sendCitizenOtp = ({ stateCode, flow, mobileNumber, ...details }) =>
  Digit.UserService.sendOtp(
    {
      otp: {
        ...details,
        mobileNumber,
        tenantId: stateCode,
        userType: CITIZEN_USER_TYPE,
        type: flow,
      },
    },
    stateCode
  );

export const authenticateCitizen = async ({ stateCode, mobileNumber, otp }) => {
  const { ResponseInfo, UserRequest: info, ...tokens } = await Digit.UserService.authenticate({
    username: mobileNumber,
    password: otp,
    tenantId: stateCode,
    userType: CITIZEN_USER_TYPE,
  });

  // Preserve the existing single-instance tenant normalization for both UIs.
  if (window?.globalConfigs?.getConfig("ENABLE_SINGLEINSTANCE")) {
    info.tenantId = Digit.ULBService.getStateId();
  }
  return { info, ...tokens };
};

export const registerCitizen = async ({ stateCode, mobileNumber, otp, name }) => {
  const { ResponseInfo, UserRequest: info, ...tokens } = await Digit.UserService.registerUser(
    {
      name,
      username: mobileNumber,
      otpReference: otp,
      tenantId: stateCode,
    },
    stateCode
  );

  if (window?.globalConfigs?.getConfig("ENABLE_SINGLEINSTANCE")) {
    info.tenantId = Digit.ULBService.getStateId();
  }
  return { info, ...tokens };
};

// Persist the same DIGIT and backward-compatible citizen session keys used by
// V1 so downstream modules cannot distinguish a V2-authenticated citizen.
export const persistCitizenSession = (user, tenantId) => {
  const locale = JSON.parse(sessionStorage.getItem("Digit.initData"))?.value?.selectedLanguage;
  Digit.SessionStorage.set("citizen.userRequestObject", user);
  Digit.UserService.setUser(user);
  localStorage.setItem("Citizen.tenant-id", tenantId);
  localStorage.setItem("tenant-id", tenantId);
  localStorage.setItem("citizen.userRequestObject", JSON.stringify(user?.info));
  localStorage.setItem("locale", locale);
  localStorage.setItem("Citizen.locale", locale);
  localStorage.setItem("token", user?.access_token);
  localStorage.setItem("Citizen.token", user?.access_token);
  localStorage.setItem("user-info", JSON.stringify(user?.info));
  localStorage.setItem("Citizen.user-info", JSON.stringify(user?.info));
};

export const getCitizenTenantCode = (city) => {
  if (!city || typeof city !== "object") return city;
  return city.code || city.value || city.tenantId;
};

// V2 collects the home city before authentication, so persist it through the
// same session key consumed by ULBService and the citizen dashboard.
export const persistCitizenHomeCity = (city) => {
  const code = getCitizenTenantCode(city);
  if (!code) return;
  Digit.SessionStorage.set("CITIZEN.COMMON.HOME.CITY", typeof city === "object" ? { ...city, code } : { code });
};

// Reuse the V1 DigiLocker authorization endpoint and verifier storage. The
// browser redirect remains owned by the existing DigiLocker service response.
export const startCitizenDigiLockerLogin = async () => {
  const data = await Digit.DigiLockerService.register({ module: DIGILOCKER_MODULE });
  localStorage.setItem("code_verfier_register", data?.dlReqRef);
  window.location.assign(data.redirectURL);
};

const formatDigiLockerDate = (dateValue) => {
  if (!dateValue || dateValue.length !== 8) return dateValue;
  return `${dateValue.slice(0, 2)}/${dateValue.slice(2, 4)}/${dateValue.slice(4, 8)}`;
};

// Complete the same token -> user-check -> OAuth sequence currently used by
// V1. The original redirect URI is retained because it must match the OAuth request.
export const completeCitizenDigiLockerLogin = async ({ code, stateCode }) => {
  const tokenData = await Digit.DigiLockerService.token({
    TokenReq: {
      code_verifier: localStorage.getItem("code_verfier_register"),
      code,
      module: DIGILOCKER_MODULE,
      redirect_uri: DEFAULT_DIGILOCKER_REDIRECT_URI,
    },
  });
  const tokenResponse = tokenData?.TokenRes;
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
    if (!isCitizenAlreadyRegisteredError(registrationError)) throw registrationError;
    await sendCitizenOtp({ stateCode, flow: "login", mobileNumber: citizenDetails.mobileNumber });
  }

  const authData = await Digit.DigiLockerService.oauth({
    access_token: tokenResponse?.access_token,
    tenantId: stateCode,
    digilockerid: tokenResponse?.digilockerid,
    name: tokenResponse?.name,
    dob: citizenDetails.dob,
    gender: tokenResponse?.gender,
    mobileNumber: tokenResponse?.mobile,
  });
  const { UserRequest: info, ...tokens } = authData;
  return { info, ...tokens };
};

export const getCitizenAuthError = (error, fallback = "Unable to complete authentication. Please try again.") => {
  return error?.response?.data?.Errors?.[0]?.message || error?.response?.data?.error_description || error?.message || fallback;
};
