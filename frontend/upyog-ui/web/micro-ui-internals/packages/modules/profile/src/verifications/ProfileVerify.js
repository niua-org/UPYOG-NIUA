import React, { useState, useEffect, useRef } from "react";
import { useTranslation } from "react-i18next";
import { Card, CardLabel, TextInput, SubmitBar, Toast, OTPInput } from "@nudmcdgnpm/digit-ui-react-components";
import { PROFILE_ROUTES, OTP_LENGTH } from "../utils/constants";

/**
 * Component Developed for Profile Verification of the Users who register
 * through Mobile Number, user can enter their aadhar number, otp will be sent 
 * to their registered mobile number with aadhar, after verifying otp, user profile will be verified
 */


const ProfileVerify = () => {
  const navigate = Digit.Hooks.useCustomNavigate();
  const { t } = useTranslation();

  const [aadhaar, setAadhaar] = useState("");
  const [otp, setOtp] = useState("");
  const [referenceId, setReferenceId] = useState(null);
  const [loading, setLoading] = useState(false);
  const [verifying, setVerifying] = useState(false);
  const [toast, setToast] = useState(null);

  const navigationTimerRef = useRef(null);

  const tenantId = Digit.ULBService.getCurrentTenantId();

  // Auto-dismiss toast
  useEffect(() => {
    if (!toast) return;

    const timer = setTimeout(() => setToast(null), 5000);
    return () => clearTimeout(timer);
  }, [toast]);

  // Clear pending navigation timer on unmount
  useEffect(() => {
    return () => {
      if (navigationTimerRef.current) {
        clearTimeout(navigationTimerRef.current);
      }
    };
  }, []);

  const showError = (label) => setToast({ error: true, label });

  const handleAadhaarChange = (e) => {
    const rawValue = e.target.value.replace(/\D/g, "").slice(0, 12);

    setAadhaar(rawValue);
    setReferenceId(null);
    setOtp("");
  };

  // Send Aadhaar OTP
  const sendAadhaarOtp = async () => {
    if (aadhaar.length !== 12) {
      showError("PROFILE_AADHAR_INVALID");
      return;
    }

    setLoading(true);

    try {
      const res = await Digit.UserService.sendAadhaarOtp({
        aadhaarNumber: aadhaar,
      });

      if (res?.status !== "OTP_SENT" || !res?.referenceId) {
        throw new Error("OTP_NOT_SENT");
      }

      setReferenceId(res.referenceId);
      setOtp("");
    } catch (err) {
      showError(err?.response?.data?.status || t("AADHAAR_OTP_SEND_FAILED"));
    } finally {
      setLoading(false);
    }
  };

  // Verify Profile OTP
  const handleVerify = async () => {
    setVerifying(true);
    try {
      const res = await Digit.UserService.verifyProfile({
        referenceId,
        otp,
        aadhaarNumber: aadhaar,
        tenantId,
      });

      if (res?.user?.isIdentityVerified !== true) {
        throw new Error("PROFILE_VERIFICATION_FAILED");
      }

      // 1. Whitelist only required non-sensitive fields from verified user
      const verifiedFields = {
        name: res.user.name,
        isIdentityVerified: res.user.isIdentityVerified,
        ...(res.user.gender && { gender: res.user.gender }),
        ...(res.user.dob && { dob: res.user.dob }),
      };

      const sessionUser = Digit.UserService.getUser();
      if (sessionUser) {
        // 2. Align user shape: keep existing tokens and merge only whitelisted fields into info
        const updatedUser = {
          ...sessionUser,
          info: {
            ...sessionUser.info,
            ...verifiedFields,
          },
        };

        // 3. Update via standard Digit helpers (SessionStorage)
        Digit.UserService.setUser(updatedUser);
        Digit.SessionStorage.set("citizen.userRequestObject", updatedUser);

        // 4. Update documented UPYOG localStorage keys (only safe info object, no raw aadhaar)
        localStorage.setItem("user-info", JSON.stringify(updatedUser.info));
        localStorage.setItem("Citizen.user-info", JSON.stringify(updatedUser.info));
      }

      setToast({
        error: false,
        label: t("PROFILE_VERIFICATION_SUCCESS"),
      });

      navigationTimerRef.current = setTimeout(() => {
        window.location.href = PROFILE_ROUTES.DETAILS;
      }, 2000);

    } catch (err) {
      console.log("Error while verifying Profile:", err);
      showError(err?.response?.data?.Errors?.[0]?.message || t("PROFILE_VERIFICATION_FAILED"));
      setOtp("");
      setVerifying(false);
    }
  };

  return (
    <React.Fragment>
      <Card>
        <CardLabel>{t("CORE_COMMON_AADHAR_NUMBER")}</CardLabel>

        <TextInput type="tel" name="aadhaar" value={aadhaar.replace(/(\d{4})(?=\d)/g, "$1 ")} onChange={handleAadhaarChange} placeholder={t("PROFILE_AADHAR_PLACEHOLDER")} />

        <SubmitBar label={t("COMMON_SEARCH")} onSubmit={sendAadhaarOtp} disabled={loading || aadhaar.length !== 12 || !!referenceId} />

        {referenceId && (
          <React.Fragment>
            <CardLabel>{t("CORE_COMMON_ENTER_OTP")}</CardLabel>

            <OTPInput length={OTP_LENGTH} value={otp} onChange={setOtp} />

            <SubmitBar label={t("CORE_COMMON_VERIFY_OTP")} onSubmit={handleVerify} disabled={otp.length !== OTP_LENGTH || verifying} />
          </React.Fragment>
        )}
      </Card>

      {toast && <Toast error={toast.error} label={toast.label} onClose={() => setToast(null)} />}
    </React.Fragment>
  );
};

export default ProfileVerify;
