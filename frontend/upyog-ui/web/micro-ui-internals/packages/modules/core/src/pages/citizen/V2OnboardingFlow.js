import { useEffect, useState } from "react";
import { Navigate, useLocation, useNavigate, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Toast } from "@nudmcdgnpm/digit-ui-react-components";
import LoginV2 from "../onboarding/LoginV2";
import OtpV2 from "../onboarding/OtpV2";
import RegisterV2 from "../onboarding/RegisterV2";
import { useOnboarding } from "../onboarding/OnboardingContext";
import {
  authenticateCitizen,
  completeCitizenDigiLockerLogin,
  getCitizenAuthError,
  isCitizenNotRegisteredError,
  persistCitizenHomeCity,
  persistCitizenSession,
  registerCitizen,
  sendCitizenOtp,
  startCitizenDigiLockerLogin,
} from "./Login/citizenAuth";
import DigiLockerConsentModal from "./Login/DigiLockerConsentModal";
import { CITIZEN_BASE_PATH, CITIZEN_DASHBOARD_PATH, getCitizenOnboardingPaths, isValidCitizenV2Flow } from "./onboardingRoutes";

// City is the canonical selected location in the current MDMS contract. Accept
// a distinct location when future configs provide one without persisting a
// duplicate alias containing the same tenant object.
const getCitizenLocation = ({ city, location }) => location || city;
const hasRequiredLoginContext = (data) =>
  Boolean(data.mobileNumber && data.city?.code && getCitizenLocation(data)?.code);

export const CitizenLoginV2 = ({ stateCode }) => {
  const navigate = useNavigate();
  const routerLocation = useLocation();
  const { t } = useTranslation();
  const { clearFormData, formData, updateFormData } = useOnboarding();
  const [error, setError] = useState(null);
  const [isRegistrationNavigationPending, setIsRegistrationNavigationPending] = useState(false);
  const [showDigiLockerConsent, setShowDigiLockerConsent] = useState(false);
  const onboardingPaths = getCitizenOnboardingPaths(true);

  useEffect(() => {
    if (!isRegistrationNavigationPending || formData._registrationAllowed !== true) return;

    // Mount the protected register route only after its context marker has
    // committed, otherwise the guard can redirect the new user back to login.
    setIsRegistrationNavigationPending(false);
    navigate(onboardingPaths.register, { replace: true });
  }, [formData._registrationAllowed, isRegistrationNavigationPending, navigate, onboardingPaths.register]);

  const onSubmit = async (data) => {
    const { mobileNumber, city, language } = data;
    // Required-field failures remain owned by ConfigurableForm and are shown
    // below the field; Toast is reserved for API/authentication failures.
    if (!mobileNumber || !city?.code || !language) return;

    const loginContext = {
      ...data,
      _from: routerLocation.state?.from,
      _otpSent: false,
      _otpFlow: null,
      _registrationAllowed: false,
    };
    // Begin a fresh flow so obsolete aliases/markers from an earlier session
    // cannot leak into the new login context.
    clearFormData();
    updateFormData(loginContext);
    persistCitizenHomeCity(city);
    setIsRegistrationNavigationPending(false);
    setError(null);

    try {
      await sendCitizenOtp({ stateCode, flow: "login", mobileNumber });
      updateFormData({ _otpSent: true, _otpFlow: "login" });
      navigate(onboardingPaths.loginOtp, { state: { mobileNumber }, replace: true });
    } catch (loginOtpError) {
      if (!isCitizenNotRegisteredError(loginOtpError)) {
        setError(getCitizenAuthError(loginOtpError, "Unable to send login OTP."));
        return;
      }

      // Only the backend's explicit unregistered-user response may enter the
      // registration branch; all other OTP failures stay on login with Toast.
      updateFormData({ _registrationAllowed: true });
      setIsRegistrationNavigationPending(true);
    }
  };

  // Secondary action is exclusively an alternate authentication mechanism;
  // registration remains a result of the primary login/user check.
  const startDigiLocker = async () => {
    try {
      setError(null);
      setShowDigiLockerConsent(false);
      await startCitizenDigiLockerLogin();
    } catch (digiLockerError) {
      setError(getCitizenAuthError(digiLockerError, "Unable to start DigiLocker login."));
    }
  };

  useEffect(() => {
    const searchParams = new URLSearchParams(routerLocation.search);
    const code = searchParams.get("code");
    if (!code) {
      if (searchParams.has("error")) setError("DigiLocker login was not completed.");
      return;
    }

    const completeDigiLocker = async () => {
      try {
        const user = await completeCitizenDigiLockerLogin({ code, stateCode });
        persistCitizenSession(user, user?.info?.tenantId || stateCode);
        persistCitizenHomeCity(user?.info?.permanentCity || stateCode);
        navigate(CITIZEN_DASHBOARD_PATH, { replace: true });
      } catch (digiLockerError) {
        setError(getCitizenAuthError(digiLockerError, "Unable to complete DigiLocker login."));
      }
    };
    completeDigiLocker();
  }, [navigate, routerLocation.search, stateCode]);

  return (
    <>
      <LoginV2 onSubmit={onSubmit} onSecondaryAction={() => setShowDigiLockerConsent(true)} />
      <DigiLockerConsentModal
        isOpen={showDigiLockerConsent}
        onCancel={() => setShowDigiLockerConsent(false)}
        onConfirm={startDigiLocker}
        t={t}
      />
      {error && <Toast error isDleteBtn label={t(error)} onClose={() => setError(null)} />}
    </>
  );
};

export const CitizenRegisterV2 = ({ stateCode }) => {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { formData, updateFormData } = useOnboarding();
  const [error, setError] = useState(null);
  const onboardingPaths = getCitizenOnboardingPaths(true);

  // Registration is intentionally protected because V2 always begins at
  // CitizenLoginV2, which supplies the mobile number and selected city context.
  if (!hasRequiredLoginContext(formData) || formData._registrationAllowed !== true) {
    return <Navigate to={onboardingPaths.entry} replace />;
  }

  const handleRegister = async (registrationData) => {
    const completeRegistrationData = { ...formData, ...registrationData };
    updateFormData(completeRegistrationData);
    setError(null);

    try {
      // V1 registration sends a type=register OTP after collecting the user's
      // registration fields. V2 reuses that request and continues to register OTP.
      await sendCitizenOtp({
        stateCode,
        flow: "register",
        mobileNumber: formData.mobileNumber,
        name: completeRegistrationData.fullName,
        dob: completeRegistrationData.dob,
      });
      updateFormData({ _otpSent: true, _otpFlow: "register" });
      navigate(onboardingPaths.registerOtp, { replace: true });
    } catch (registrationError) {
      setError(getCitizenAuthError(registrationError, "Unable to start registration verification."));
    }
  };

  return (
    <>
      {/* RegisterV2 has only the primary registration action; alternate login
          actions remain the responsibility of CitizenLoginV2. */}
      <RegisterV2 onSubmit={handleRegister} />
      {/* Registration API failures use the same Toast feedback pattern as V1. */}
      {error && <Toast isDleteBtn error label={t(error)} onClose={() => setError(null)} />}
    </>
  );
};

export const CitizenOtpV2 = ({ stateCode }) => {
  const { flow } = useParams();
  const { t } = useTranslation();
  const { clearFormData, formData } = useOnboarding();
  const [error, setError] = useState(null);
  const onboardingPaths = getCitizenOnboardingPaths(true);
  const hasOtpContext =
    hasRequiredLoginContext(formData) && formData._otpSent === true && formData._otpFlow === flow;

  // OTP routes are reachable only after the matching login/register OTP request.
  // replace prevents Back/Forward from repeatedly restoring an invalid OTP URL.
  if (!isValidCitizenV2Flow(flow) || !hasOtpContext) {
    return <Navigate to={onboardingPaths.entry} replace />;
  }

  const completeAuthentication = (user) => {
    persistCitizenHomeCity(formData.city);
    persistCitizenSession(user, stateCode);
    clearFormData();
    // Reload the standard citizen entry after persisting authentication. On
    // NIUATT this resolves to https://niuatt.niua.in/upyog-ui/citizen, while
    // relative routing keeps the same behavior valid in every environment.
    window.location.replace(CITIZEN_BASE_PATH);
  };

  const handleOtpSubmit = async (otpData) => {
    setError(null);
    try {
      // The route parameter selects the same V1 verification operation:
      // authenticate for login, registerUser for registration.
      const user =
        flow === "login"
          ? await authenticateCitizen({
              stateCode,
              mobileNumber: formData.mobileNumber,
              otp: otpData.otp,
            })
          : await registerCitizen({
              stateCode,
              mobileNumber: formData.mobileNumber,
              otp: otpData.otp,
              name: formData.fullName,
            });
      completeAuthentication(user);
    } catch (otpError) {
      setError(getCitizenAuthError(otpError, "CS_INVALID_OTP"));
    }
  };

  const handleResend = async () => {
    try {
      setError(null);
      await sendCitizenOtp({
        stateCode,
        flow,
        mobileNumber: formData.mobileNumber,
        ...(flow === "register" ? { name: formData.fullName, dob: formData.dob } : {}),
      });
    } catch (resendError) {
      setError(getCitizenAuthError(resendError, "Unable to resend OTP."));
    }
  };

  return (
    <>
      <OtpV2 mobileNumber={formData.mobileNumber} onSubmit={handleOtpSubmit} onResend={handleResend} />
      {/* OTP verification and resend failures remain visible until dismissed. */}
      {error && <Toast isDleteBtn error label={t(error)} onClose={() => setError(null)} />}
    </>
  );
};
