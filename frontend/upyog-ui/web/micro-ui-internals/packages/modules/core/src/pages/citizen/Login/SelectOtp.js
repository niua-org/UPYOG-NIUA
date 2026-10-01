import React, { useState, Fragment, useEffect } from "react";
import { ButtonSelector, CardText, FormStep, LinkButton, OTPInput, CardLabelError } from "@nudmcdgnpm/digit-ui-react-components";
import useInterval from "../../../hooks/useInterval";
import { completeCitizenDigiLockerLogin, persistCitizenSession } from "./citizenAuth";

const SelectOtp = ({ config, otp, onOtpChange, onResend, onSelect, t, error, userType = "citizen", canSubmit }) => {
  const [timeLeft, setTimeLeft] = useState(30);
  const [errorRegister, setErrorRegister]= useState(false)
  useInterval(
    () => {
      setTimeLeft(timeLeft - 1);
    },
    timeLeft > 0 ? 1000 : null
  );
  useEffect(() => {
    const handleDigiLocker = async () => {
      const searchParams = new URLSearchParams(window.location.search);
      const code = searchParams.get("code");
      if (code) {
        try {
          // V1 now consumes the shared DigiLocker exchange used by V2.
          const stateCode = Digit.ULBService.getStateId();
          const user = await completeCitizenDigiLockerLogin({ code, stateCode });
          persistCitizenSession(user, user?.info?.tenantId || stateCode);
          window.location.replace("/upyog-ui/citizen");
        } catch {
          setErrorRegister(true);
        }
      } else if (searchParams.has("error")) {
        window.location.replace(window.location.pathname.split("/otp")[0]);
      }
    };
    handleDigiLocker();
  }, [])
  const handleResendOtp = () => {
    onResend();
    setTimeLeft(2);
  };
  if (userType === "employee") {
    return (
      <Fragment>
        <OTPInput length={6} onChange={onOtpChange} value={otp} />
        {timeLeft > 0 ? (
          <CardText>{`${t("CS_RESEND_ANOTHER_OTP")} ${timeLeft} ${t("CS_RESEND_SECONDS")}`}</CardText>
        ) : (
          <p className="card-text-button" onClick={handleResendOtp}>
            {t("CS_RESEND_OTP")}
          </p>
        )}
        {!error && <CardLabelError>{t("CS_INVALID_OTP")}</CardLabelError>}
      </Fragment>
    );
  }

  return (
    <FormStep onSelect={onSelect} config={config} t={t} isDisabled={!(otp?.length === 6 && canSubmit)}>
      <OTPInput length={6} onChange={onOtpChange} value={otp} />
      {timeLeft > 0 ? (
        <CardText>{`${t("CS_RESEND_ANOTHER_OTP")} ${timeLeft} ${t("CS_RESEND_SECONDS")}`}</CardText>
      ) : (
        <p className="card-text-button" onClick={handleResendOtp}>
          {t("CS_RESEND_OTP")}
        </p>
      )}
      {!error && <CardLabelError>{t("CS_INVALID_OTP")}</CardLabelError>}
      {errorRegister && <CardLabelError>{t("CS_ALREADY_REGISTERED")}</CardLabelError>}
    </FormStep>)
};

export default SelectOtp;
