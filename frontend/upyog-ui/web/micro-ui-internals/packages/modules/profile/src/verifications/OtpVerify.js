import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { CardLabel, SubmitBar, OTPInput } from "@nudmcdgnpm/digit-ui-react-components";
import { OTP_LENGTH } from "../utils/constants";

/**
 * Generic OTP entry + verify step.
 * OTP entry + verify step for both flows.
 * type: "AADHAAR" | "FAMILY_ID" decides which verify API is called.
 * onVerified(response) is called when the response has isVerified === true.
 */


const OtpVerify = ({ type, referenceId, userUuid, familyId, onVerified, onError }) => {
  const { t } = useTranslation();
  const tenantId = Digit.ULBService.getCurrentTenantId();
  const [otp, setOtp] = useState("");
  const [verifying, setVerifying] = useState(false);

  const verifyOtp = () =>
    type === "FAMILY_ID"
      ? Digit.UserService.verifyFamilyOtp(familyId, tenantId, otp, referenceId)
      : Digit.UserService.verifyAadhaarOtp({ referenceId, otp, isResync: false, userUuid });

  const handleVerify = async () => {
    setVerifying(true);
    try {
      const res = await verifyOtp();
      if (!res?.isVerified) throw new Error("OTP_NOT_VERIFIED");
      onVerified(res); // parent navigates away, so no state reset needed
    } catch (err) {
      onError(err?.Errors?.[0]?.description || t("OTP_VERIFY_FAILED"));
      setOtp("");
      setVerifying(false);
    }
  };

  return (
    <div>
      <CardLabel>{t("CORE_COMMON_ENTER_OTP")}</CardLabel>
      <OTPInput length={OTP_LENGTH} onChange={setOtp} value={otp} />
      <SubmitBar
        label={t("CORE_COMMON_VERIFY_OTP")}
        onSubmit={handleVerify}
        disabled={otp.length !== OTP_LENGTH || verifying}
      />
    </div>
  );
};

export default OtpVerify;