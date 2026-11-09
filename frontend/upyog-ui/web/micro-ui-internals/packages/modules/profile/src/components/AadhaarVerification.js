import React, { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import {
  CardLabel,
  CardLabelError,
  TextInput,
  OTPInput,
  SubmitBar,
  Loader,
  EditIcon,
} from "@nudmcdgnpm/digit-ui-react-components";
import { profileStyles } from "../utils/profileStyles";
import { buildVerifiedMemberInfo } from "../utils/payloads";

/**
 * AadhaarVerification Component
 *
 * Reusable single-card Aadhaar verification widget.
 *
 * Capabilities:
 * 1. Formatted 12-digit Aadhaar input (XXXX XXXX XXXX).
 * 2. Informational consent callout banner.
 * 3. Triggers Aadhaar OTP via `Digit.UserService.sendAadhaarOtp`.
 * 4. Displays masked mobile number to which OTP was dispatched.
 * 5. 30-second countdown timer for resending OTP.
 * 6. "Edit Aadhaar Number" action to cancel current OTP session and unlock the input.
 * 7. Verifies 6-digit OTP via `Digit.UserService.verifyAadhaarOtp` and triggers `onVerified`.
 *
 * @param {Object} props
 * @param {string} [props.label] - Custom input label for Aadhaar number field
 * @param {boolean} [props.showConsentBox=true] - Whether to display the informational Aadhaar consent box
 * @param {(data: { verifiedMember: Object, aadhaarLast4: string, maskedMobile: string }) => void} props.onVerified - Callback invoked on successful OTP verification
 * @param {(msg: string) => void} [props.onError] - Callback invoked when an error occurs during OTP send or verify
 * @returns {JSX.Element} Rendered Aadhaar verification card content
 */
const AadhaarVerification = ({
  label,
  showConsentBox = true,
  onVerified,
  onError,
}) => {
  const { t } = useTranslation();
  const styles = profileStyles.aadhaarVerification;
  const stateId = Digit.ULBService.getStateId();
  const tenantId = Digit.ULBService.getCurrentTenantId() || stateId;

  // Form input and session state
  const [aadhaarNumber, setAadhaarNumber] = useState("");
  const [cleanAadhaar, setCleanAadhaar] = useState("");
  const [isOtpSent, setIsOtpSent] = useState(false);
  const [referenceId, setReferenceId] = useState(null);
  const [txnId, setTxnId] = useState(null);
  const [maskedMobile, setMaskedMobile] = useState("+91 ******XXXX");
  const [otp, setOtp] = useState("");
  const [error, setError] = useState(null);

  // 30s countdown timer state
  const [timeLeft, setTimeLeft] = useState(30);
  const [timerActive, setTimerActive] = useState(false);
  const [loading, setLoading] = useState(false);

  // Countdown timer effect
  useEffect(() => {
    let interval = null;
    if (timerActive && timeLeft > 0) {
      interval = setInterval(() => setTimeLeft((prev) => prev - 1), 1000);
    } else if (timeLeft === 0) {
      setTimerActive(false);
    }
    return () => clearInterval(interval);
  }, [timerActive, timeLeft]);

  /**
   * Format raw digits into 4-digit grouped Aadhaar string: "1234 5678 9012"
   */
  const handleAadhaarChange = (e) => {
    const rawVal = e.target.value.replace(/\D/g, "").slice(0, 12);
    setCleanAadhaar(rawVal);
    const formatted = rawVal.replace(/(\d{4})(?=\d)/g, "$1 ").trim();
    setAadhaarNumber(formatted);
    if (error) setError(null);
  };

  /**
   * Step 1: Send OTP to Aadhaar-registered mobile
   */
  const handleSendOtp = async () => {
    if (cleanAadhaar.length !== 12) {
      const err = t("CORE_COMMON_PROFILE_AADHAR_NUMBER_INVALID");
      setError(err);
      onError?.(err);
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const requestData = { aadhaarNumber: cleanAadhaar };
      const res = await Digit.UserService.sendAadhaarOtp(requestData);

      setTxnId(res?.txnId || null);
      setReferenceId(res?.referenceId || res?.txnId || null);

      if (res?.mobileNumber) {
        setMaskedMobile(`+91 ******${res.mobileNumber.slice(-4)}`);
      }
      setTimeLeft(30);
      setTimerActive(true);
      setIsOtpSent(true);
    } catch (err) {
      const errorMsg =
        err?.response?.data?.message ||
        err?.response?.data?.Errors?.[0]?.message ||
        err?.message ||
        t("CS_SOMETHING_WENT_WRONG");
      setError(errorMsg);
      onError?.(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Resend OTP handler triggered after 30-second cooldown
   */
  const handleResendOtp = async () => {
    if (timerActive && timeLeft > 0) return;
    setOtp("");
    setError(null);
    await handleSendOtp();
  };

  /**
   * Reset OTP session and unlock Aadhaar input for corrections
   */
  const handleEditAadhaar = () => {
    setIsOtpSent(false);
    setOtp("");
    setReferenceId(null);
    setTxnId(null);
    setError(null);
    setTimerActive(false);
    setTimeLeft(30);
  };

  /**
   * Step 2: Verify OTP submitted by citizen
   */
  const handleVerifyOtp = async () => {
    if (otp.length !== 6) {
      const err = t("CS_INVALID_OTP");
      setError(err);
      onError?.(err);
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const userInfo = Digit.UserService.getUser()?.info || {};
      const requestData = {
        RequestInfo: {
          apiId: "org.upyog.dx",
          action: "_verifyOtp",
        },
        referenceId: referenceId || txnId,
        otp: otp,
        isResync: false,
        userUuid: userInfo?.uuid,
        tenantId: tenantId,
      };

      const response = await Digit.UserService.verifyAadhaarOtp(requestData);
      const aadhaarLast4 = cleanAadhaar ? cleanAadhaar.slice(-4) : "";
      const verifiedMember = buildVerifiedMemberInfo({ response, aadhaarLast4 });

      onVerified?.({ verifiedMember, aadhaarLast4, maskedMobile });
    } catch (err) {
      const errorMsg =
        err?.response?.data?.message ||
        err?.response?.data?.Errors?.[0]?.message ||
        err?.message ||
        t("CS_INVALID_OTP");
      setError(errorMsg);
      onError?.(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      {/* Aadhaar Input Field */}
      <CardLabel>
        {label || t("CORE_COMMON_ENTER_AADHAAR")}
        <span className="check-page-link-button"> *</span>
      </CardLabel>

      <TextInput
        type="text"
        name="aadhaarNumber"
        value={aadhaarNumber}
        onChange={handleAadhaarChange}
        placeholder={t("1234 5678 9012")}
        inputMode="numeric"
        maxLength={14}
        disable={isOtpSent || loading}
      />

      {/* Aadhaar Consent Banner */}
      {showConsentBox && (
        <div style={styles.consentBox}>
          <span style={styles.consentIcon}>💡</span>
          <span style={styles.consentText}>
            {t("AADHAAR_OTP_INFO_CALLOUT")}
          </span>
        </div>
      )}

      {/* Send OTP Button (visible before OTP dispatch) */}
      {!isOtpSent && (
        <SubmitBar
          label={loading ? t("COMMON_SENDING") : t("CS_SEND_OTP", "Send OTP")}
          onSubmit={handleSendOtp}
          disabled={cleanAadhaar.length !== 12 || loading}
        />
      )}

      {/* OTP Verification Section (appears inline below upon OTP dispatch) */}
      {isOtpSent && (
        <div style={styles.otpSection}>
          <CardLabel>{`${t("CS_ENTER_OTP_SENT_TO")} ${maskedMobile}`}</CardLabel>
          <OTPInput
            length={6}
            onChange={(val) => {
              setOtp(val);
              if (error) setError(null);
            }}
            value={otp}
          />

          {/* 30s Countdown Timer / Resend OTP Action */}
          <div style={styles.otpTimerWrap}>
            {timeLeft > 0 ? (
              <p style={styles.otpTimerText}>
                {`${t("CS_RESEND_ANOTHER_OTP")} ${timeLeft} ${t("CS_RESEND_SECONDS")}`}
              </p>
            ) : (
              <p
                className="card-text-button cp"
                style={styles.otpResendBtn}
                onClick={handleResendOtp}
              >
                {t("CS_RESEND_OTP")}
              </p>
            )}
          </div>

          {/* OTP Action Row: Verify OTP and Edit Aadhaar Button */}
          <div style={styles.otpActionRow}>
            <SubmitBar
              label={loading ? t("COMMON_VERIFYING") : t("CS_VERIFY_OTP", "Verify OTP")}
              onSubmit={handleVerifyOtp}
              disabled={otp.length !== 6 || loading}
            />
            <button
              type="button"
              onClick={handleEditAadhaar}
              style={styles.editAadhaarBtn}
            >
              <EditIcon style={styles.editAadhaarIcon} />
              {t("CORE_COMMON_EDIT_AADHAAR_NUMBER")}
            </button>
          </div>
        </div>
      )}

      {/* Inline Field Error */}
      {error && <CardLabelError>{error}</CardLabelError>}

      {/* Loading Overlay */}
      {loading && <Loader />}
    </div>
  );
};

export default AadhaarVerification;
