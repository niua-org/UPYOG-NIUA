import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { CardLabel, SubmitBar, OTPInput } from "@nudmcdgnpm/digit-ui-react-components";
import { OTP_LENGTH } from "../utils/constants";

/**
 * Generic OTP entry + verify step.
 * - onVerify(otp): async fn that calls the flow-specific verify API and returns its response
 * - onVerified(response): called when the response has isVerified === true
 * - onError(message): called on any failure
 */


const OtpVerify = ({ referenceId, userUuid, onVerified, onError }) => {
  const { t } = useTranslation();
  const [otp, setOtp] = useState("");
  const [verifying, setVerifying] = useState(false);

  const handleVerify = async () => {
    setVerifying(true);
    try {
      const res = await Digit.UserService.verifyAadhaarOtp({
        referenceId,
        otp,
        isResync: false,
        userUuid,
      });
      if (!res?.isVerified) throw new Error("AADHAAR_NOT_VERIFIED");
      onVerified(res); // parent swaps this component out, so no state reset needed
    } catch (err) {
      onError(t("AADHAAR_OTP_VERIFY_FAILED"));
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