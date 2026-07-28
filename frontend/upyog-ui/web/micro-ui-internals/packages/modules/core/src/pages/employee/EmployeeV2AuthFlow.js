import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { Toast } from "@nudmcdgnpm/digit-ui-react-components";
import OnboardingForm from "../onboarding/OnboardingForm";
import { useOnboarding } from "../onboarding/OnboardingContext";
import { maskMobileNumber } from "../onboarding/formUtils";
import { getEmployeeAuthPaths } from "./employeeAuthRoutes";

// Normalize the different error envelopes returned by employee login, OTP and
// password-reset APIs into one message contract for the shared Toast.
const getEmployeeAuthError = (error) =>
  error?.response?.data?.error_description ||
  error?.response?.data?.error?.fields?.[0]?.message ||
  error?.response?.data?.Errors?.[0]?.message ||
  error?.message ||
  "Invalid login credentials!";

const EmployeeConfiguredStepV2 = ({ step, onSubmit, onSecondaryAction, onResend, descriptionSuffix }) => {
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

  // Retain the same DIGIT and backward-compatible storage keys consumed by
  // existing employee modules after authentication.
  Digit.SessionStorage.set("citizen.userRequestObject", user);
  Digit.SessionStorage.set("Employee.tenantId", tenantId);
  Digit.UserService.setUser(user);
  localStorage.setItem("Employee.tenant-id", tenantId);
  localStorage.setItem("tenant-id", tenantId);
  localStorage.setItem("citizen.userRequestObject", JSON.stringify(user?.info));
  localStorage.setItem("locale", locale);
  localStorage.setItem("Employee.locale", locale);
  localStorage.setItem("token", user?.access_token);
  localStorage.setItem("Employee.token", user?.access_token);
  localStorage.setItem("user-info", JSON.stringify(user?.info));
  localStorage.setItem("Employee.user-info", JSON.stringify(user?.info));
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
export const EmployeeLoginV2 = () => {
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
      <EmployeeConfiguredStepV2
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
export const EmployeeForgotPasswordV2 = () => {
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
      {/* EmployeeConfiguredStepV2 resolves the forgot-password MDMS schema and
          delegates its validated submission to the OTP request above. */}
      <EmployeeConfiguredStepV2 step="forgot-password" onSubmit={handleForgotPassword} />
      {error && <Toast error isDleteBtn label={error} onClose={() => setError(null)} />}
    </>
  );
};

// Complete the guarded, non-logged-in employee password reset using the mobile
// and tenant context created by EmployeeForgotPasswordV2.
export const EmployeeChangePasswordV2 = () => {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const [error, setError] = useState(null);
  const { clearFormData, formData } = useOnboarding();
  const mobileNumber = formData.mobileNumber;
  const tenantId = formData.city?.code;
  // Direct-access guards, resend requests and completion all use V2 auth paths.
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
    } catch (resendError) {
      setError(getEmployeeAuthError(resendError));
      // Re-throw so OnboardingForm/Otp field logic knows the resend failed and
      // does not present it as a successful timer reset.
      throw resendError;
    }
  };

  const handleChangePassword = async (data, _config, { setFieldError } = {}) => {
    const userName = data.userName || data.username || mobileNumber;
    // Accept common MDMS field aliases, then normalize them to the backend's
    // fixed non-logged-in password-reset contract.
    const otpReference = data.otpReference || data.otp || data.otpNumber;
    const newPassword = data.newPassword || data.password;
    const confirmPassword = data.confirmPassword || data.confirmNewPassword;
    const confirmPasswordField = data.confirmPassword !== undefined ? "confirmPassword" : "confirmNewPassword";

    if (!userName || !otpReference || !newPassword || !confirmPassword) return;
    if (newPassword !== confirmPassword) {
      // A password mismatch is a field validation problem, so keep it inline
      // and focus the confirm field instead of displaying an API-error Toast.
      setFieldError?.(confirmPasswordField, t("ERR_PASSWORD_DO_NOT_MATCH"));
      return;
    }

    setError(null);
    try {
      // Submit only fields accepted by /user/password/nologin/_update. In
      // particular, do not let an existing session switch this reset flow to
      // the authenticated password endpoint.
      await Digit.UserService.changePassword(
        {
          userName,
          otpReference,
          newPassword,
          tenantId,
          type: "EMPLOYEE",
        },
        tenantId,
        { withoutLogin: true }
      );
      // Reset context is no longer valid after success; clearing it also keeps
      // passwords/OTP data out of future onboarding sessions.
      clearFormData();
      navigate(employeeAuthPaths.login, { replace: true });
    } catch (changePasswordError) {
      setError(getEmployeeAuthError(changePasswordError));
    }
  };

  return (
    <>
      {/* The shared form supplies password and OTP fields plus resend UI; this
          adapter supplies Employee-specific reset and validation behavior. */}
      <EmployeeConfiguredStepV2
        step="change-password"
        onSubmit={handleChangePassword}
        onResend={handleResend}
        descriptionSuffix={maskedMobileNumber}
      />
      {error && <Toast error isDleteBtn label={error} onClose={() => setError(null)} />}
    </>
  );
};
