import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { Toast } from "@nudmcdgnpm/digit-ui-react-components";
import OnboardingForm from "../onboarding/OnboardingForm";
import { useOnboarding } from "../onboarding/OnboardingContext";
import { maskMobileNumber } from "../onboarding/formUtils";
import { persistAuthSession } from "../onboarding/authSession";
import { getEmployeeAuthPaths } from "./AuthRoutes";

// Normalize the different error envelopes returned by employee login, OTP and
// password-reset APIs into one message contract for the shared Toast.
const getEmployeeAuthError = (error) =>
  error?.response?.data?.error_description ||
  error?.response?.data?.error?.fields?.[0]?.message ||
  error?.response?.data?.Errors?.[0]?.message ||
  error?.message ||
  "Invalid login credentials!";

const ConfiguredStepV2 = ({ step, onSubmit, onSecondaryAction, onResend, descriptionSuffix }) => {
  // OnboardingForm owns the reusable loading and missing-config states. This
  // adapter only connects Employee authentication actions to a configured step.
  return (
    <OnboardingForm
      step={step}
      onSubmit={onSubmit}
      onSecondaryAction={onSecondaryAction}
      onResend={onResend}
      descriptionSuffix={descriptionSuffix}
    />
  );
};

// Persist the V2 login result under the same keys used by V1 and existing
// employee modules so changing the authentication UI does not change sessions.
const persistEmployeeSession = (user) => {
  const locale = Digit.SessionStorage.get("locale") || "en_IN";
  const tenantId = user?.info?.tenantId;

  // Reuse the common key map and request Employee's additional session tenant
  // entry, which existing employee modules still consume.
  persistAuthSession({
    user,
    tenantId,
    locale,
    storageNamespace: "Employee",
    sessionTenantKey: "Employee.tenantId",
  });
};

// Retain the legacy role-specific landing pages and honor a protected route's
// saved `from` destination for all other successfully authenticated employees.
const getPostLoginPath = (user, requestedPath) => {
  const roles = user?.info?.roles || [];
  if (roles.length && roles.every((role) => role.code === "NATADMIN")) {
    return "/upyog-ui/employee/dss/landing/NURT_DASHBOARD";
  }
  if (roles.length && roles.every((role) => role.code === "STADMIN")) {
    return "/upyog-ui/employee/dss/landing/home";
  }
  return requestedPath || "/upyog-ui/employee";
};

// Bind the configured employee login step to the existing password-grant API
// and version-aware forgot-password navigation.
export const LoginV2 = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [error, setError] = useState(null);
  // This screen exists only under /user/v2, so its secondary and success paths
  // must always be selected from the V2 employee route set.
  const employeeAuthPaths = getEmployeeAuthPaths(true);

  const handleLogin = async (data) => {
    const username = data.username || data.userName;
    const { password, city } = data;
    if (!username || !password || !city?.code) return;

    setError(null);
    try {
      // Use the same password-grant request as Employee V1 while submitting
      // only authentication fields from the configuration-driven V2 form.
      const { UserRequest: info, ...tokens } = await Digit.UserService.authenticate({
        username,
        password,
        tenantId: city.code,
        userType: "EMPLOYEE",
      });
      const tenantRoles = info?.roles?.filter((role) => role.tenantId === info.tenantId);
      if (tenantRoles?.length) info.roles = tenantRoles;

      const user = { info, ...tokens };
      persistEmployeeSession(user);
      navigate(getPostLoginPath(user, location.state?.from), { replace: true });
    } catch (loginError) {
      // Authentication failures use Toast; field validation remains inline in
      // ConfigurableForm and never enters this API error path.
      setError(getEmployeeAuthError(loginError));
    }
  };

  return (
    <>
      {/* The shared form renders MDMS fields; this adapter supplies employee
          login and makes forgot-password the configured secondary action. */}
      <ConfiguredStepV2
        step="login"
        onSubmit={handleLogin}
        onSecondaryAction={() => navigate(employeeAuthPaths.forgotPassword)}
      />
      {error && <Toast error isDleteBtn label={error} onClose={() => setError(null)} />}
    </>
  );
};

// Request a password-reset OTP from the configured mobile/city fields, then
// persist only the non-sensitive continuation values needed by the next step.
export const ForgotPasswordV2 = () => {
  const navigate = useNavigate();
  const [error, setError] = useState(null);
  const { updateFormData } = useOnboarding();
  // Keep forgot-password and change-password navigation within /user/v2.
  const employeeAuthPaths = getEmployeeAuthPaths(true);

  const handleForgotPassword = async (data) => {
    const { mobileNumber, city } = data;
    if (!mobileNumber || !city?.code) return;

    setError(null);
    try {
      // Reuse the existing Employee forgot-password request contract. The
      // password-reset OTP is issued before continuing to the current
      // configured change-password step inside the same V2 route family.
      await Digit.UserService.sendOtp(
        {
          otp: {
            mobileNumber,
            userType: "citizen",
            type: "passwordreset",
            tenantId: city.code,
          },
        },
        city.code
      );

      // Keep reset identity/location in the persisted onboarding state rather
      // than exposing mobile_number and tenantId in the browser URL.
      updateFormData({ mobileNumber, city });
      // Do not replace this entry: Back may intentionally return the user to
      // the configured forgot-password form to correct their details.
      navigate(employeeAuthPaths.changePassword);
    } catch (forgotPasswordError) {
      // OTP API failures are global feedback; required mobile/city validation
      // continues to render below the corresponding configured fields.
      setError(getEmployeeAuthError(forgotPasswordError));
    }
  };

  return (
    <>
      {/* ConfiguredStepV2 resolves the forgot-password MDMS schema and
          delegates its validated submission to the OTP request above. */}
      <ConfiguredStepV2 step="forgot-password" onSubmit={handleForgotPassword} />
      {error && <Toast error isDleteBtn label={error} onClose={() => setError(null)} />}
    </>
  );
};

// Complete the guarded, non-logged-in employee password reset using the mobile
// and tenant context created by EmployeeForgotPasswordV2.
export const ChangePasswordV2 = () => {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const [error, setError] = useState(null);
  const { formData } = useOnboarding();
  const mobileNumber = formData.mobileNumber;
  const tenantId = formData.city?.code;
  // Direct-access guards and reset navigation stay within the configured flow.
  const employeeAuthPaths = getEmployeeAuthPaths(true);
  // Keep the full mobile number in onboarding state and expose only its
  // masked form in the configured OTP description.
  const maskedMobileNumber = maskMobileNumber(mobileNumber);

  // Change-password is reachable only after forgot-password has stored the
  // mobile number and city in onboarding state. Invalid direct access returns
  // to the configured V2 forgot-password step without relying on URL parameters.
  if (!mobileNumber || !tenantId) {
    return <Navigate to={employeeAuthPaths.forgotPassword} replace />;
  }

  const handleResend = async () => {
    setError(null);
    try {
      await Digit.UserService.sendOtp(
        {
          otp: {
            mobileNumber,
            userType: Digit.UserService.getType().toUpperCase(),
            type: "passwordreset",
            tenantId,
          },
        },
        tenantId
      );
      setError(t("ES_OTP_RESEND"));
    } catch (resendError) {
      setError(resendError?.response?.data?.error_description || t("ES_INVALID_LOGIN_CREDENTIALS"));
      // Notify the configurable OTP field that resend failed so its timer stays
      // available for another attempt.
      throw resendError;
    } finally {
      window.setTimeout(() => setError(null), 5000);
    }
  };

  const handleChangePassword = async (data) => {
    try {
      if (data.newPassword !== data.confirmPassword) {
        return setError(t("ERR_PASSWORD_DO_NOT_MATCH"));
      }

      const { otpReference: submittedOtpReference, otp, otpNumber, ...passwordData } = data;
      const requestData = {
        ...passwordData,
        otpReference: submittedOtpReference || otp || otpNumber,
        tenantId,
        type: Digit.UserService.getType().toUpperCase(),
      };

      await Digit.UserService.changePassword(requestData, tenantId);
      navigate("/upyog-ui/employee/user/login", { replace: true });
    } catch (changePasswordError) {
      setError(changePasswordError?.response?.data?.error?.fields?.[0]?.message || t("ES_SOMETHING_WRONG"));
      window.setTimeout(() => setError(null), 5000);
    }
  };

  return (
    <>
      {/* The shared form supplies password and OTP fields plus resend UI; this
          adapter supplies Employee-specific reset and validation behavior. */}
      <ConfiguredStepV2
        step="change-password"
        onSubmit={handleChangePassword}
        onResend={handleResend}
        descriptionSuffix={maskedMobileNumber}
      />
      {error && <Toast error isDleteBtn label={error} onClose={() => setError(null)} />}
    </>
  );
};
